import { act, render } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../services/projectStatus'
import { useLiveProjectStatus } from './useLiveProjectStatus'

vi.mock('../services/projectStatus', () => ({
    subscribeProjectStatus: vi.fn(),
}))

const status = (compileState: ProjectStatusUpdate['compileState']): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState,
})

const Probe = ({ enabled, initial }: { enabled: boolean, initial: ProjectStatusUpdate | null }) => {
    const live = useLiveProjectStatus('p1', 'main', enabled, initial)
    return <span data-testid="state">{live?.compileState ?? 'none'}</span>
}

describe('useLiveProjectStatus', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() } as never)
    })

    it('subscribes, seeds from the initial status and applies pushed updates when enabled', async () => {
        let onUpdate!: (update: ProjectStatusUpdate) => void
        vi.mocked(subscribeProjectStatus).mockImplementation((_id, _branch, listener) => {
            onUpdate = listener
            return { unsubscribe: vi.fn() } as never
        })

        const { getByTestId } = render(<Probe enabled initial={status('ok')} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        expect(getByTestId('state').textContent).toBe('ok')

        act(() => onUpdate(status('errors')))
        expect(getByTestId('state').textContent).toBe('errors')
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
