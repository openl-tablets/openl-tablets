import traceService from 'services/traceService'
import { useTraceStore } from 'store/traceStore'
import type { MockedFunction } from 'vitest'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getVariables: vi.fn(),
        getStack: vi.fn(),
    },
}))

vi.mock('antd', () => ({
    __esModule: true,
    notification: { error: vi.fn() },
}))

function deferred<T>() {
    let resolve!: (value: T) => void
    const promise = new Promise<T>(r => {
        resolve = r
    })
    return { promise, resolve }
}

const getVariables = traceService.getVariables as MockedFunction<typeof traceService.getVariables>
const getStack = traceService.getStack as MockedFunction<typeof traceService.getStack>

describe('traceStore race hardening', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('ignores a late variables response after the user selects another frame', async () => {
        const vars0 = { parameters: [], steps: [], errors: []} as any
        const vars1 = { parameters: [], steps: [], errors: []} as any
        const slowFrame0 = deferred<any>()
        getVariables.mockImplementation((_projectId: string, index: number) =>
            index === 0 ? slowFrame0.promise : Promise.resolve(vars1)
        )

        useTraceStore.setState({ status: 'SUSPENDED', stackVersion: 1 })
        const store = useTraceStore.getState()

        const pending = store.selectFrame(0) // parks on the slow response
        await store.selectFrame(1) // resolves with frame 1's variables
        slowFrame0.resolve(vars0) // frame 0's response arrives late
        await pending

        expect(useTraceStore.getState().selectedFrameIndex).toBe(1)
        expect(useTraceStore.getState().variables).toBe(vars1)
        expect(useTraceStore.getState().variablesLoading).toBe(false)
    })

    it('clears the variables spinner when the session terminates mid-fetch', async () => {
        const slowFrame = deferred<any>()
        getVariables.mockReturnValue(slowFrame.promise)

        useTraceStore.setState({ status: 'SUSPENDED', stackVersion: 1, selectedFrameIndex: 0 })
        const pending = useTraceStore.getState().selectFrame(0) // parks with variablesLoading=true

        // The worker finishes (or errors/terminates) while the variables fetch is still in flight.
        useTraceStore.getState().onSocketStatus('COMPLETED')
        slowFrame.resolve({ parameters: [], steps: [], errors: [] } as any)
        await pending

        const state = useTraceStore.getState()
        expect(state.variablesLoading).toBe(false) // no spinner stuck on a finished session
        expect(state.variables).toBeNull() // the stale frame's variables are dropped
    })

    it('drops variables fetched against a superseded suspension', async () => {
        const vars0 = { parameters: [], steps: [], errors: []} as any
        const slowFrame0 = deferred<any>()
        getVariables.mockReturnValue(slowFrame0.promise)

        useTraceStore.setState({ status: 'SUSPENDED', stackVersion: 1 })
        const pending = useTraceStore.getState().selectFrame(0)

        // A new suspension advances the stack while the fetch is in flight.
        useTraceStore.setState({ stackVersion: 2 })
        slowFrame0.resolve(vars0)
        await pending

        expect(useTraceStore.getState().variables).toBeNull()
    })

    it('skips the duplicate stack refresh while a synchronous step is in flight', () => {
        getStack.mockResolvedValue({ status: 'SUSPENDED', frames: []} as any)
        const store = useTraceStore.getState()

        useTraceStore.setState({ loading: true })
        store.onSocketStatus('SUSPENDED')
        expect(getStack).not.toHaveBeenCalled()

        useTraceStore.setState({ loading: false })
        store.onSocketStatus('SUSPENDED')
        expect(getStack).toHaveBeenCalledTimes(1)
    })

    it('surfaces an immediate error summary from the socket message', () => {
        getStack.mockResolvedValue({ status: 'ERROR', frames: []} as any)

        useTraceStore.getState().onSocketStatus('ERROR', 'Something failed')

        const state = useTraceStore.getState()
        expect(state.status).toBe('ERROR')
        expect(state.debugError).toEqual({ summary: 'Something failed' })
        expect(state.frames).toEqual([])
    })

    it('loads the cleaned, located error from the stack after a failure', async () => {
        const debugError = { summary: 'Division by zero', table: 'CalcRate', type: 'ArithmeticException' }
        getStack.mockResolvedValue({ status: 'ERROR', frames: [], error: debugError } as any)

        await useTraceStore.getState().fetchTerminalError()

        expect(useTraceStore.getState().debugError).toEqual(debugError)
    })
})
