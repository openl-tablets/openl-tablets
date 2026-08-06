import { act, render } from '@testing-library/react'
import { useRef } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'
import { useLiveProjectStatus } from './useLiveProjectStatus'

// Only the channel is stubbed; the freshness rule stays the one the screens use.
vi.mock('../services/projectStatus', async importOriginal => ({
    ...(await importOriginal<typeof import('../services/projectStatus')>()),
    subscribeProjectStatus: vi.fn(),
}))

const status = (compileState: ProjectStatusUpdate['compileState'], message?: string): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState,
    ...(message === undefined ? {} : {
        compilation: {
            messages: { total: 1, errors: 1, warnings: 0, items: [{ id: 1, summary: message, severity: 'ERROR', stacktrace: false }]},
        },
    }),
})

const Probe = ({ enabled, initial, readAt }: {
    enabled: boolean
    initial: ProjectStatusUpdate | null
    readAt?: number
}) => {
    const live = useLiveProjectStatus('p1', 'main', enabled, initial, readAt)
    return <span data-testid="state">{live?.compileState ?? 'none'}</span>
}

const Messages = ({ initial, readAt }: { initial: ProjectStatusUpdate, readAt?: number }) => {
    const live = useLiveProjectStatus('p1', 'main', true, initial, readAt)
    return <span data-testid="message">{live?.compilation?.messages?.items?.[0]?.summary ?? 'none'}</span>
}

/** Captures the channel listener, so a test can push a status by hand. */
const captureUpdates = () => {
    let onUpdate!: (update: ProjectStatusUpdate) => void
    vi.mocked(subscribeProjectStatus).mockImplementation((_id, _branch, listener) => {
        onUpdate = listener
        return { unsubscribe: vi.fn() } as never
    })
    return (update: ProjectStatusUpdate) => onUpdate(update)
}

describe('useLiveProjectStatus', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() } as never)
    })

    it('subscribes, seeds from the initial status and applies pushed updates when enabled', async () => {
        const push = captureUpdates()

        const { getByTestId } = render(<Probe enabled initial={status('ok')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        expect(getByTestId('state').textContent).toBe('ok')

        act(() => push(status('errors')))
        expect(getByTestId('state').textContent).toBe('errors')
    })

    it('gives way to a read that started after the push, so a lost update does not stick', async () => {
        const push = captureUpdates()

        const { getByTestId, rerender } = render(<Probe enabled initial={status('idle')} readAt={1_000} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        // The channel says the project is compiling, which is newer than the read behind the screen.
        act(() => push(status('compiling')))
        expect(getByTestId('state').textContent).toBe('compiling')

        // A read started after that push carries the answer the server gives now - the finished compile
        // whose push never arrived - so the pushed status steps aside.
        rerender(<Probe enabled initial={status('ok')} readAt={Date.now() + 1_000} />)
        expect(getByTestId('state').textContent).toBe('ok')
    })

    it('keeps the push that arrived while the read behind the screen was still in flight', async () => {
        const push = captureUpdates()
        // A moment before the push, so the two cannot fall into the same millisecond and tie.
        const readStartedAt = Date.now() - 1_000

        const { getByTestId, rerender } = render(<Probe enabled initial={status('idle')} readAt={readStartedAt} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        act(() => push(status('compiling')))

        // The read that was already running lands afterwards with what the server knew before the push.
        rerender(<Probe enabled initial={status('ok')} readAt={readStartedAt} />)

        expect(getByTestId('state').textContent).toBe('compiling')
    })

    it('answers with the same object when a re-read brings back the same status', async () => {
        const Identity = ({ initial }: { initial: ProjectStatusUpdate }) => {
            const live = useLiveProjectStatus('p1', 'main', true, initial)
            const seen = useRef<ProjectStatusUpdate | null>(null)
            const stable = seen.current === null || seen.current === live
            seen.current = live
            return <span data-testid="stable">{String(stable)}</span>
        }

        // A read carries an equal status in a new object - the panels derive their message lists and
        // their paging from it, so a new identity would reset what the user expanded.
        const { getByTestId, rerender } = render(<Identity initial={status('errors')} />)
        rerender(<Identity initial={status('errors')} />)

        expect(getByTestId('stable').textContent).toBe('true')
    })

    it('answers again when only the message texts changed, not their counts', async () => {
        const push = captureUpdates()
        const { getByTestId, rerender } = render(<Messages initial={status('errors', 'the old error')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        // The author fixed one error and introduced another, so the counts and the state are the same.
        act(() => push(status('errors', 'the new error')))
        expect(getByTestId('message').textContent).toBe('the new error')

        // The same holds for a read that brings the new text.
        rerender(<Messages initial={status('errors', 'a third error')} readAt={Date.now() + 1_000} />)
        expect(getByTestId('message').textContent).toBe('a third error')
    })

    it('leaves a pushed status alone when a read carries none at all', async () => {
        const push = captureUpdates()
        const { getByTestId, rerender } = render(<Probe enabled initial={status('idle')} readAt={1_000} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        act(() => push(status('errors')))

        // The server holds no compilation for the project, so its answer says nothing about it.
        rerender(<Probe enabled initial={null} readAt={Date.now() + 1_000} />)

        expect(getByTestId('state').textContent).toBe('errors')
    })

    it('drops a push of the branch left behind, even when the new one looks just like it', async () => {
        const push = captureUpdates()
        // Every value the hook answers with, including the render committed before the resubscribe
        // effect runs - that one is painted, so the branch left behind must not show through it.
        const answered: string[] = []
        const Branch = ({ branch, initial }: { branch: string, initial: ProjectStatusUpdate }) => {
            const live = useLiveProjectStatus('p1', branch, true, initial, Date.now() - 1_000)
            answered.push(live?.compilation?.messages?.items?.[0]?.summary ?? 'none')
            return null
        }

        const { rerender } = render(<Branch branch="main" initial={status('errors', 'from main')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        act(() => push(status('errors', 'pushed on main')))
        answered.length = 0

        // A branch just cut from main: same state, same counts, its own messages.
        rerender(<Branch branch="feature" initial={status('errors', 'from feature')} />)

        expect(answered).not.toContain('pushed on main')
        expect(answered.at(-1)).toBe('from feature')
    })

    it('does not subscribe when disabled', async () => {
        const { getByTestId } = render(<Probe enabled={false} initial={status('ok')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).not.toHaveBeenCalled()
        expect(getByTestId('state').textContent).toBe('none')
    })

    it('unsubscribes on unmount', async () => {
        const unsubscribe = vi.fn()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe } as never)

        const { unmount } = render(<Probe enabled initial={status('ok')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        unmount()

        expect(unsubscribe).toHaveBeenCalled()
    })
})
