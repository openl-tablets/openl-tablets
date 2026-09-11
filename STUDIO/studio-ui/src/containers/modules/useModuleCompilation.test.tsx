import { act, render } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../../services/projectStatus'
import { startModuleCompilation } from '../../services/modules'
import { useModuleCompilation } from './useModuleCompilation'

// Only the channel and the request are stubbed; the readiness rule stays the one the editor runs on.
vi.mock('../../services/projectStatus', async importOriginal => ({
    ...(await importOriginal<typeof import('../../services/projectStatus')>()),
    subscribeProjectStatus: vi.fn(),
}))

vi.mock('../../services/modules', () => ({ startModuleCompilation: vi.fn() }))

const compiling = (compiled: number, total: number, ...names: string[]): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState: 'compiling',
    compilation: { modules: { compiled, total, compiledModules: names } },
})

const Probe = ({ initial, reloadToken }: {
    initial?: ProjectStatusUpdate | null
    reloadToken?: number
}) => {
    const { ready, compiled, total, failure } = useModuleCompilation(
        'p1', 'main', 'Claims', initial ?? null, 0, reloadToken
    )
    return (
        <span data-testid="state">
            {`${ready ? 'ready' : 'waiting'} ${compiled}/${total} ${failure ?? '-'}`}
        </span>
    )
}

/** Captures the channel listener, so a test can push a status by hand. */
const captureUpdates = () => {
    let onUpdate!: (update: ProjectStatusUpdate) => void
    vi.mocked(subscribeProjectStatus).mockImplementation((_id, _branch, listener) => {
        onUpdate = listener
        return { unsubscribe: vi.fn() } as never
    })
    return (update: ProjectStatusUpdate) => act(() => onUpdate(update))
}

describe('useModuleCompilation', () => {
    beforeEach(() => {
        vi.mocked(startModuleCompilation).mockResolvedValue(undefined)
    })

    it('asks for the compilation and waits, reporting how far it has come', async () => {
        const push = captureUpdates()
        const { getByTestId } = render(<Probe />)

        expect(startModuleCompilation).toHaveBeenCalledWith('p1', 'Claims')
        push(compiling(3, 12, 'Pricing'))

        expect(getByTestId('state').textContent?.trim()).toEqual('waiting 3/12 -')
    })

    it('is ready the moment the channel names this module, whatever the rest are still doing', async () => {
        const push = captureUpdates()
        const { getByTestId } = render(<Probe />)

        push(compiling(4, 12, 'Pricing', 'Claims'))

        // Four of twelve: the modules after this one are still compiling, and the editor opens anyway.
        expect(getByTestId('state').textContent?.trim()).toEqual('ready 4/12 -')
    })

    it('asks for no compilation when the module is already compiled', async () => {
        captureUpdates()
        const { getByTestId } = render(<Probe initial={compiling(12, 12, 'Claims')} />)

        expect(startModuleCompilation).not.toHaveBeenCalled()
        expect(getByTestId('state').textContent?.trim()).toEqual('ready 12/12 -')
    })

    it('asks again on a refresh, even for a module already compiled', async () => {
        captureUpdates()
        const { rerender } = render(<Probe initial={compiling(12, 12, 'Claims')} reloadToken={0} />)
        expect(startModuleCompilation).not.toHaveBeenCalled()

        rerender(<Probe initial={compiling(12, 12, 'Claims')} reloadToken={1} />)

        expect(startModuleCompilation).toHaveBeenCalledWith('p1', 'Claims')
    })

    it('asks once, however many statuses arrive', async () => {
        const push = captureUpdates()
        render(<Probe />)

        push(compiling(1, 12))
        push(compiling(2, 12))
        push(compiling(3, 12, 'Pricing'))

        expect(startModuleCompilation).toHaveBeenCalledTimes(1)
    })

    it('reports a compilation that could not even be asked for', async () => {
        captureUpdates()
        vi.mocked(startModuleCompilation).mockRejectedValue(new Error('no such module'))

        const { getByTestId, findByText } = render(<Probe />)

        await findByText(/no such module/)
        expect(getByTestId('state').textContent?.trim()).toEqual('waiting 0/0 no such module')
    })
})
