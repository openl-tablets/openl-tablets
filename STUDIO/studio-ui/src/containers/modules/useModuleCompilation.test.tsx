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

const Probe = ({ initial, reloadToken, branch, enabled }: {
    initial?: ProjectStatusUpdate | null
    reloadToken?: number
    branch?: string | null
    enabled?: boolean
}) => {
    const { ready, compiled, total, failure, tests } = useModuleCompilation(
        'p1', branch === undefined ? 'main' : branch, 'Claims', initial ?? null, 0, reloadToken, enabled ?? true
    )
    return (
        <span data-testid="state">
            {`${ready ? 'ready' : 'waiting'} ${compiled}/${total} ${failure ?? '-'} tests:${tests}`}
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

    it('waits for the branch before joining the channel it is named after', () => {
        const push = captureUpdates()
        // The project has not been read yet: its branch is unknown, and so is the channel to listen on.
        const { rerender, getByTestId } = render(<Probe branch={null} enabled={false} />)
        expect(subscribeProjectStatus).not.toHaveBeenCalled()
        expect(startModuleCompilation).not.toHaveBeenCalled()

        rerender(<Probe branch="main" enabled />)

        // One channel, joined once: re-joining would throw away what a quick compilation already said.
        expect(subscribeProjectStatus).toHaveBeenCalledTimes(1)
        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        push(compiling(2, 2, 'Claims'))
        expect(getByTestId('state')).toHaveTextContent('ready 2/2')
    })

    it('asks for the compilation and waits, reporting how far it has come', async () => {
        const push = captureUpdates()
        const { getByTestId } = render(<Probe />)

        // Opening a module compiles what is not compiled yet; it does not throw away what is.
        expect(startModuleCompilation).toHaveBeenCalledWith('p1', 'Claims', false)
        push(compiling(3, 12, 'Pricing'))

        expect(getByTestId('state').textContent?.trim()).toEqual('waiting 3/12 - tests:0')
    })

    it('keeps the number of tests a full status reported while the compilation goes on reporting progress', () => {
        const push = captureUpdates()
        const { getByTestId } = render(<Probe />)

        push({
            projectId: 'p1',
            branch: 'main',
            compileState: 'ok',
            compilation: { modules: { compiled: 2, total: 2, compiledModules: ['Claims'] }, tests: { total: 7 } },
        })
        expect(getByTestId('state')).toHaveTextContent('tests:7')

        // A progress status counts no tests — walking every method for that is what it exists to avoid — and
        // the Test button must not empty and fill again with every push.
        push(compiling(1, 2, 'Claims'))

        expect(getByTestId('state')).toHaveTextContent('tests:7')
    })

    it('is ready the moment the channel names this module, whatever the rest are still doing', async () => {
        const push = captureUpdates()
        const { getByTestId } = render(<Probe />)

        push(compiling(4, 12, 'Pricing', 'Claims'))

        // Four of twelve: the modules after this one are still compiling, and the editor opens anyway.
        expect(getByTestId('state').textContent?.trim()).toEqual('ready 4/12 - tests:0')
    })

    it('asks for no compilation when the module is already compiled', async () => {
        captureUpdates()
        const { getByTestId } = render(<Probe initial={compiling(12, 12, 'Claims')} />)

        expect(startModuleCompilation).not.toHaveBeenCalled()
        expect(getByTestId('state').textContent?.trim()).toEqual('ready 12/12 - tests:0')
    })

    it('asks again on a refresh, even for a module already compiled', async () => {
        captureUpdates()
        const { rerender } = render(<Probe initial={compiling(12, 12, 'Claims')} reloadToken={0} />)
        expect(startModuleCompilation).not.toHaveBeenCalled()

        rerender(<Probe initial={compiling(12, 12, 'Claims')} reloadToken={1} />)

        // A refresh builds the module again from the workbook rather than keeping what is compiled.
        expect(startModuleCompilation).toHaveBeenCalledWith('p1', 'Claims', true)
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
        expect(getByTestId('state').textContent?.trim()).toEqual('waiting 0/0 no such module tests:0')
    })
})
