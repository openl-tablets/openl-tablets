import { act, render } from '@testing-library/react'
import { useRef } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'
import { getProject } from '../services/repositories'
import type { Project } from '../types/projects'
import { useLiveProjectStatus } from './useLiveProjectStatus'

// Only the channel is stubbed; the freshness rule stays the one the screens use.
vi.mock('../services/projectStatus', async importOriginal => ({
    ...(await importOriginal<typeof import('../services/projectStatus')>()),
    subscribeProjectStatus: vi.fn(),
}))

vi.mock('../services/repositories', () => ({ getProject: vi.fn() }))

const status = (
    compileState: ProjectStatusUpdate['compileState'],
    sequence: number,
    message?: string
): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState,
    sequence,
    ...(message === undefined ? {} : {
        compilation: {
            messages: { total: 1, errors: 1, warnings: 0, items: [{ id: 1, summary: message, severity: 'ERROR', stacktrace: false }]},
        },
    }),
})

const Probe = ({ enabled = true, initial, branch = 'main' }: {
    enabled?: boolean
    initial: ProjectStatusUpdate | null
    branch?: string
}) => {
    const live = useLiveProjectStatus('p1', branch, enabled, initial)
    return <span data-testid="state">{live?.compileState ?? 'none'}</span>
}

const Messages = ({ initial }: { initial: ProjectStatusUpdate }) => {
    const live = useLiveProjectStatus('p1', 'main', true, initial)
    return <span data-testid="message">{live?.compilation?.messages?.items?.[0]?.summary ?? 'none'}</span>
}

/** Captures the channel listeners, so a test can push a status, or start the subscription, by hand. */
const captureChannel = () => {
    let onUpdate!: (update: ProjectStatusUpdate) => void
    let onStarted: ((sequence: number) => void) | undefined
    vi.mocked(subscribeProjectStatus).mockImplementation((_id, _branch, listener, started) => {
        onUpdate = listener
        onStarted = started
        return { unsubscribe: vi.fn() }
    })
    return {
        push: (update: ProjectStatusUpdate) => act(() => onUpdate(update)),
        start: async (sequence: number) => {
            await act(async () => {
                onStarted?.(sequence)
                await settle()
            })
        },
    }
}

const settle = () => new Promise(resolve => setTimeout(resolve, 0))

const mount = async (element: React.ReactElement) => {
    const rendered = render(element)
    await act(settle)
    return rendered
}

describe('useLiveProjectStatus', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() })
    })

    it('subscribes, seeds from the initial status and applies pushed updates when enabled', async () => {
        const { push } = captureChannel()

        const { getByTestId } = await mount(<Probe initial={status('ok', 10)} />)

        expect(subscribeProjectStatus)
            .toHaveBeenCalledWith('p1', 'main', expect.any(Function), expect.any(Function))
        expect(getByTestId('state').textContent).toBe('ok')

        push(status('errors', 11))
        expect(getByTestId('state').textContent).toBe('errors')
    })

    it('gives way to a status the server read after the push, so a lost update does not stick', async () => {
        const { push } = captureChannel()
        const { getByTestId, rerender } = await mount(<Probe initial={status('idle', 10)} />)

        push(status('compiling', 11))
        expect(getByTestId('state').textContent).toBe('compiling')

        // The read the server answered later carries the finished compile whose push never arrived.
        rerender(<Probe initial={status('ok', 12)} />)
        expect(getByTestId('state').textContent).toBe('ok')
    })

    it('keeps a push the server sent after the read that lands later', async () => {
        const { push } = captureChannel()
        const { getByTestId, rerender } = await mount(<Probe initial={status('idle', 10)} />)
        push(status('compiling', 12))

        // The read was answered before the push, though its answer arrives after it.
        rerender(<Probe initial={status('ok', 11)} />)

        expect(getByTestId('state').textContent).toBe('compiling')
    })

    it('answers with the same object when a re-read brings back the same status', () => {
        const Identity = ({ initial }: { initial: ProjectStatusUpdate }) => {
            const live = useLiveProjectStatus('p1', 'main', true, initial)
            const seen = useRef<ProjectStatusUpdate | null>(null)
            const stable = seen.current === null || seen.current === live
            seen.current = live
            return <span data-testid="stable">{String(stable)}</span>
        }

        // A read carries an equal status in a new object, numbered anew - the panels derive their message lists
        // and their paging from it, so a new identity would reset what the user expanded.
        const { getByTestId, rerender } = render(<Identity initial={status('errors', 10)} />)
        rerender(<Identity initial={status('errors', 11)} />)

        expect(getByTestId('stable').textContent).toBe('true')
    })

    it('answers again when only the message texts changed, not their counts', async () => {
        const { push } = captureChannel()
        const { getByTestId, rerender } = await mount(<Messages initial={status('errors', 10, 'the old error')} />)

        // The author fixed one error and introduced another, so the counts and the state are the same.
        push(status('errors', 11, 'the new error'))
        expect(getByTestId('message').textContent).toBe('the new error')

        // The same holds for a read that brings the new text.
        rerender(<Messages initial={status('errors', 12, 'a third error')} />)
        expect(getByTestId('message').textContent).toBe('a third error')
    })

    it('leaves a pushed status alone when a read carries none at all', async () => {
        const { push } = captureChannel()
        const { getByTestId, rerender } = await mount(<Probe initial={status('idle', 10)} />)
        push(status('errors', 11))

        // The server holds no compilation for the project, so its answer says nothing about it.
        rerender(<Probe initial={null} />)

        expect(getByTestId('state').textContent).toBe('errors')
    })

    it('drops a push of the branch left behind, even when the new one looks just like it', async () => {
        const { push } = captureChannel()
        // Every value the hook answers with, including the render committed before the resubscribe
        // effect runs - that one is painted, so the branch left behind must not show through it.
        const answered: string[] = []
        const Branch = ({ branch, initial }: { branch: string, initial: ProjectStatusUpdate }) => {
            const live = useLiveProjectStatus('p1', branch, true, initial)
            answered.push(live?.compilation?.messages?.items?.[0]?.summary ?? 'none')
            return null
        }

        const { rerender } = await mount(<Branch branch="main" initial={status('errors', 10, 'from main')} />)
        push(status('errors', 12, 'pushed on main'))
        answered.length = 0

        // A branch just cut from main: same state, same counts, its own messages, read before the push.
        rerender(<Branch branch="feature" initial={{ ...status('errors', 11, 'from feature'), branch: 'feature' }} />)

        expect(answered).not.toContain('pushed on main')
        expect(answered.at(-1)).toBe('from feature')
    })

    it('does not subscribe when disabled', async () => {
        const { getByTestId } = await mount(<Probe enabled={false} initial={status('ok', 10)} />)

        expect(subscribeProjectStatus).not.toHaveBeenCalled()
        expect(getByTestId('state').textContent).toBe('none')
    })

    it('reads the status again when the subscription started after it was read', async () => {
        const { start } = captureChannel()
        vi.mocked(getProject).mockResolvedValue({ compileStatus: status('compiling', 30) } as Project)
        const { getByTestId } = await mount(<Probe initial={status('ok', 10)} />)

        // A compilation began between the read and the start of the subscription: nothing was pushed of it.
        await start(20)

        expect(getProject).toHaveBeenCalledTimes(1)
        expect(getByTestId('state').textContent).toBe('compiling')
    })

    it('trusts a status read after the subscription started', async () => {
        const { start } = captureChannel()
        await mount(<Probe initial={status('ok', 30)} />)

        await start(20)

        expect(getProject).not.toHaveBeenCalled()
    })

    it('waits for the read on its way, and reads again when it turns out older than the subscription', async () => {
        const { start } = captureChannel()
        vi.mocked(getProject).mockResolvedValue({ compileStatus: status('compiling', 30) } as Project)
        const { getByTestId, rerender } = await mount(<Probe initial={null} />)

        await start(20)
        expect(getProject).not.toHaveBeenCalled()

        rerender(<Probe initial={status('ok', 10)} />)
        await act(settle)

        expect(getProject).toHaveBeenCalledTimes(1)
        expect(getByTestId('state').textContent).toBe('compiling')
    })

    it('reads the status again once per start, as after a reconnect', async () => {
        const { start } = captureChannel()
        vi.mocked(getProject).mockRejectedValueOnce(new Error('offline'))
            .mockResolvedValue({ compileStatus: status('warnings', 40) } as Project)
        const { getByTestId, rerender } = await mount(<Probe initial={status('compiling', 10)} />)

        await start(20)
        rerender(<Probe initial={status('compiling', 10)} />)
        await act(settle)
        expect(getProject).toHaveBeenCalledTimes(1)
        expect(getByTestId('state').textContent).toBe('compiling')

        // The compilation ended while the connection was down: it is not shown as running for good.
        await start(35)

        expect(getProject).toHaveBeenCalledTimes(2)
        expect(getByTestId('state').textContent).toBe('warnings')
    })

    it('drops a status read again for a branch left behind', async () => {
        const { start } = captureChannel()
        let answer!: (project: Project) => void
        vi.mocked(getProject).mockReturnValue(new Promise(resolve => {
            answer = resolve
        }))
        const { getByTestId, rerender } = await mount(<Probe initial={status('ok', 10)} />)
        await start(20)

        rerender(<Probe branch="feature" initial={{ ...status('idle', 25), branch: 'feature' }} />)
        await act(async () => {
            answer({ compileStatus: status('errors', 30) } as Project)
            await settle()
        })

        expect(getByTestId('state').textContent).toBe('idle')
    })

    it('unsubscribes on unmount', async () => {
        const unsubscribe = vi.fn()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe })

        const { unmount } = await mount(<Probe initial={status('ok', 10)} />)
        unmount()

        expect(unsubscribe).toHaveBeenCalled()
    })
})
