import traceService from 'services/traceService'
import { useTraceStore } from 'store/traceStore'
import type { MockedFunction } from 'vitest'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getVariables: vi.fn(),
        getStack: vi.fn(),
        getRawTable: vi.fn(),
        cancelTrace: vi.fn(),
        startTrace: vi.fn(),
        setBreakpoints: vi.fn(),
        getBreakpoints: vi.fn(),
        step: vi.fn(),
        resume: vi.fn(),
        pause: vi.fn(),
        setWatches: vi.fn(),
        getWatch: vi.fn(),
        getParameterValue: vi.fn(),
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
const getRawTable = traceService.getRawTable as MockedFunction<typeof traceService.getRawTable>

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

        useTraceStore.setState({ status: 'suspended', stackVersion: 1 })
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

        useTraceStore.setState({ status: 'suspended', stackVersion: 1, selectedFrameIndex: 0 })
        const pending = useTraceStore.getState().selectFrame(0) // parks with variablesLoading=true

        // The worker finishes (or errors/terminates) while the variables fetch is still in flight.
        useTraceStore.getState().onSocketStatus('completed')
        slowFrame.resolve({ parameters: [], steps: [], errors: []} as any)
        await pending

        const state = useTraceStore.getState()
        expect(state.variablesLoading).toBe(false) // no spinner stuck on a finished session
        expect(state.variables).toBeNull() // the stale frame's variables are dropped
    })

    it('loads the executed tree and hot-spots profile when a profiling run completes', async () => {
        const profile = { hotspots: [], distinctTables: 2, nodeCount: 100, totalMillis: 8, truncated: false } as any
        getStack.mockResolvedValue({ status: 'completed', frames: [], tree: null, profile } as any)

        useTraceStore.setState({ profiling: true })
        useTraceStore.getState().onSocketStatus('completed')
        // The socket only carries the status, so a profiling completion fetches the settled stack.
        expect(getStack).toHaveBeenCalledWith('p1')

        await new Promise(resolve => setTimeout(resolve, 0))
        expect(useTraceStore.getState().profile).toEqual(profile)
    })

    it('does not fetch the settled stack when a non-profiling run completes', () => {
        useTraceStore.setState({ profiling: false })
        useTraceStore.getState().onSocketStatus('completed')
        expect(getStack).not.toHaveBeenCalled()
    })

    it('does not fetch the stack of a terminated profiling session (it is gone, e.g. toggling profiling)', () => {
        useTraceStore.setState({ profiling: true })
        useTraceStore.getState().onSocketStatus('terminated')
        expect(getStack).not.toHaveBeenCalled()
    })

    it('drops variables fetched against a superseded suspension', async () => {
        const vars0 = { parameters: [], steps: [], errors: []} as any
        const slowFrame0 = deferred<any>()
        getVariables.mockReturnValue(slowFrame0.promise)

        useTraceStore.setState({ status: 'suspended', stackVersion: 1 })
        const pending = useTraceStore.getState().selectFrame(0)

        // A new suspension advances the stack while the fetch is in flight.
        useTraceStore.setState({ stackVersion: 2 })
        slowFrame0.resolve(vars0)
        await pending

        expect(useTraceStore.getState().variables).toBeNull()
    })

    it('skips the duplicate stack refresh while a synchronous step is in flight', () => {
        getStack.mockResolvedValue({ status: 'suspended', frames: []} as any)
        const store = useTraceStore.getState()

        useTraceStore.setState({ loading: true })
        store.onSocketStatus('suspended')
        expect(getStack).not.toHaveBeenCalled()

        useTraceStore.setState({ loading: false })
        store.onSocketStatus('suspended')
        expect(getStack).toHaveBeenCalledTimes(1)
    })

    it('surfaces an immediate error summary from the socket message', () => {
        getStack.mockResolvedValue({ status: 'error', frames: []} as any)

        useTraceStore.getState().onSocketStatus('error', 'Something failed')

        const state = useTraceStore.getState()
        expect(state.status).toBe('error')
        expect(state.debugError).toEqual({ summary: 'Something failed' })
        expect(state.frames).toEqual([])
    })

    it('loads the cleaned, located error from the stack after a failure', async () => {
        const debugError = { summary: 'Division by zero', table: 'CalcRate', type: 'ArithmeticException' }
        getStack.mockResolvedValue({ status: 'error', frames: [], error: debugError } as any)

        await useTraceStore.getState().fetchTerminalError()

        expect(useTraceStore.getState().debugError).toEqual(debugError)
    })

    it('caches a raw table for the session and serves repeats without refetching', async () => {
        const raw = { id: 'tbl', name: 'BaseRate', source: []} as any
        getRawTable.mockResolvedValue(raw)

        const first = await useTraceStore.getState().loadRawTable('tbl')
        const second = await useTraceStore.getState().loadRawTable('tbl')

        expect(first).toBe(raw)
        expect(second).toBe(raw)
        expect(getRawTable).toHaveBeenCalledTimes(1) // the second call is served from the cache
        expect(useTraceStore.getState().rawTableCache['tbl']).toBe(raw)
    })

    it('clears the raw table cache on reset', async () => {
        getRawTable.mockResolvedValue({ id: 'tbl', name: 'BaseRate', source: []} as any)
        await useTraceStore.getState().loadRawTable('tbl')

        useTraceStore.getState().reset()

        expect(useTraceStore.getState().rawTableCache).toEqual({})
    })

    it('reruns from the top: terminates, starts fresh, and re-applies the breakpoints', async () => {
        const cancelTrace = traceService.cancelTrace as MockedFunction<typeof traceService.cancelTrace>
        const startTrace = traceService.startTrace as MockedFunction<typeof traceService.startTrace>
        const setBreakpoints = traceService.setBreakpoints as MockedFunction<typeof traceService.setBreakpoints>
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session')) // start() then launches a fresh trace
        startTrace.mockResolvedValue({ status: 'suspended', frames: []} as any)
        setBreakpoints.mockResolvedValue(undefined)
        useTraceStore.setState({ breakpoints: ['uA#R1C1']})

        await useTraceStore.getState().rerun()

        expect(cancelTrace).toHaveBeenCalledWith('p1') // the old session is terminated first
        expect(startTrace).toHaveBeenCalled() // then a fresh one is started from the top
        expect(setBreakpoints).toHaveBeenCalledWith('p1', ['uA#R1C1']) // the user's breakpoints are re-applied
    })
})

const step = traceService.step as MockedFunction<typeof traceService.step>
const resume = traceService.resume as MockedFunction<typeof traceService.resume>
const pause = traceService.pause as MockedFunction<typeof traceService.pause>
const startTrace = traceService.startTrace as MockedFunction<typeof traceService.startTrace>
const cancelTrace = traceService.cancelTrace as MockedFunction<typeof traceService.cancelTrace>
const setBreakpoints = traceService.setBreakpoints as MockedFunction<typeof traceService.setBreakpoints>
const getBreakpoints = traceService.getBreakpoints as MockedFunction<typeof traceService.getBreakpoints>
const setWatches = traceService.setWatches as MockedFunction<typeof traceService.setWatches>
const getWatch = traceService.getWatch as MockedFunction<typeof traceService.getWatch>
const getParameterValue = traceService.getParameterValue as MockedFunction<typeof traceService.getParameterValue>

const suspended = (frames: any[] = []) => ({ status: 'suspended', frames, tree: null, profile: null }) as any

describe('traceStore actions', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('attaches to a session the launcher already created', async () => {
        getStack.mockResolvedValue(suspended([{ index: 0 } as any]))
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)

        await useTraceStore.getState().start()

        expect(getStack).toHaveBeenCalledWith('p1')
        expect(startTrace).not.toHaveBeenCalled() // an existing session is reused, not restarted
        expect(useTraceStore.getState().status).toBe('suspended')
    })

    it('starts a fresh session when none is attached yet', async () => {
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended())

        await useTraceStore.getState().start()

        expect(startTrace).toHaveBeenCalled()
        expect(useTraceStore.getState().status).toBe('suspended')
    })

    it('reports an error status when the trace cannot be started', async () => {
        getStack.mockRejectedValue(new Error('gone'))
        startTrace.mockRejectedValue(new Error('boom'))

        await useTraceStore.getState().start()

        const state = useTraceStore.getState()
        expect(state.status).toBe('error')
        expect(state.error).toBe('boom')
        expect(state.loading).toBe(false)
    })

    it('applies the returned stack on a successful step', async () => {
        step.mockResolvedValue(suspended([{ index: 0 } as any]))
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)
        useTraceStore.setState({ status: 'suspended' })

        await useTraceStore.getState().stepInto()

        expect(step).toHaveBeenCalledWith('p1', 'into')
        expect(useTraceStore.getState().loading).toBe(false)
    })

    it('surfaces a step failure without leaving the spinner up', async () => {
        step.mockRejectedValue(new Error('step blew up'))

        await useTraceStore.getState().stepOver()

        const state = useTraceStore.getState()
        expect(step).toHaveBeenCalledWith('p1', 'over')
        expect(state.error).toBe('step blew up')
        expect(state.loading).toBe(false)
    })

    it('rolls the status back to suspended when resume fails', async () => {
        resume.mockRejectedValue(new Error('cannot resume'))
        useTraceStore.setState({ status: 'suspended' })

        await useTraceStore.getState().resume()

        const state = useTraceStore.getState()
        expect(state.status).toBe('suspended')
        expect(state.error).toBe('cannot resume')
    })

    it('records an error when pause fails but keeps running', async () => {
        pause.mockRejectedValue(new Error('cannot pause'))
        useTraceStore.setState({ status: 'running' })

        await useTraceStore.getState().pause()

        expect(pause).toHaveBeenCalledWith('p1')
        expect(useTraceStore.getState().error).toBe('cannot pause')
    })

    it('restarts the session when profiling is toggled', async () => {
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended())

        await useTraceStore.getState().setProfiling(true)

        expect(useTraceStore.getState().profiling).toBe(true)
        expect(cancelTrace).toHaveBeenCalledWith('p1') // toggling profiling terminates and restarts
        expect(startTrace).toHaveBeenCalled()
    })

    it('ignores a profiling toggle that does not change the value', async () => {
        useTraceStore.setState({ profiling: false })

        await useTraceStore.getState().setProfiling(false)

        expect(cancelTrace).not.toHaveBeenCalled()
    })

    it('loads breakpoints and derives a label from the last path segment', async () => {
        getBreakpoints.mockResolvedValue(['file/Rules.xlsx/CalcRate'])

        await useTraceStore.getState().loadBreakpoints()

        const state = useTraceStore.getState()
        expect(state.breakpoints).toEqual(['file/Rules.xlsx/CalcRate'])
        expect(state.breakpointLabels['file/Rules.xlsx/CalcRate']).toBe('CalcRate')
    })

    it('adds a breakpoint and pushes the new set to the backend', async () => {
        setBreakpoints.mockResolvedValue(undefined)

        await useTraceStore.getState().toggleBreakpoint('uA', 'A')

        expect(useTraceStore.getState().breakpoints).toEqual(['uA'])
        expect(setBreakpoints).toHaveBeenCalledWith('p1', ['uA'])
    })

    it('rolls the breakpoint set back when the backend rejects the change', async () => {
        setBreakpoints.mockRejectedValue(new Error('nope'))
        useTraceStore.setState({ breakpoints: ['uA'], breakpointLabels: { uA: 'A' } })

        await useTraceStore.getState().toggleBreakpoint('uB', 'B')

        expect(useTraceStore.getState().breakpoints).toEqual(['uA']) // reverted to the pre-toggle set
    })

    it('registers watch cells and fetches their series', async () => {
        setWatches.mockResolvedValue(undefined)
        getWatch.mockResolvedValue({ series: []} as any)

        await useTraceStore.getState().setWatchCells(['A1'])

        expect(useTraceStore.getState().watches).toEqual(['A1'])
        expect(setWatches).toHaveBeenCalledWith('p1', ['A1'])
        expect(getWatch).toHaveBeenCalledWith('p1')
    })

    it('clears variables without fetching when selecting a frame on a non-suspended session', async () => {
        useTraceStore.setState({ status: 'running', variables: { parameters: [], steps: [], errors: []} as any })

        await useTraceStore.getState().selectFrame(0)

        expect(getVariables).not.toHaveBeenCalled()
        expect(useTraceStore.getState().variables).toBeNull()
    })

    it('marks the session running on a socket running event', () => {
        useTraceStore.setState({ status: 'suspended', loading: false })

        useTraceStore.getState().onSocketStatus('running')

        expect(useTraceStore.getState().status).toBe('running')
    })

    it('ignores a socket running event while a synchronous step owns the transition', () => {
        useTraceStore.setState({ status: 'suspended', loading: true })

        useTraceStore.getState().onSocketStatus('running')

        expect(useTraceStore.getState().status).toBe('suspended') // the in-flight step keeps ownership
    })

    it('patches a lazily-loaded parameter value into the selected frame variables', async () => {
        getParameterValue.mockResolvedValue({ parameterId: 7, value: 42 } as any)
        useTraceStore.setState({
            variables: {
                parameters: [{ parameterId: 7, value: null } as any],
                context: null,
                result: null,
                steps: [],
                errors: [],
            } as any,
        })

        const result = await useTraceStore.getState().fetchLazyParameter(7)

        expect(result.value).toBe(42)
        expect(useTraceStore.getState().variables?.parameters[0]?.value).toBe(42)
    })

    it('stores the launch route parameters', () => {
        useTraceStore.getState().setRouteParams({ projectId: 'pX', tableId: 'tX', fromModule: 'm', testRanges: '1', inputJson: '{}' })

        const state = useTraceStore.getState()
        expect(state.projectId).toBe('pX')
        expect(state.fromModule).toBe('m')
        expect(state.inputJson).toBe('{}')
    })

    it('drives step out through the backend', async () => {
        step.mockResolvedValue(suspended())
        useTraceStore.setState({ status: 'suspended' })

        await useTraceStore.getState().stepOut()

        expect(step).toHaveBeenCalledWith('p1', 'out')
    })

    it('notifies and drops variables when the variables fetch fails on a live suspension', async () => {
        getVariables.mockRejectedValue(new Error('vars boom'))
        useTraceStore.setState({ status: 'suspended', stackVersion: 1 })

        await useTraceStore.getState().selectFrame(0)

        const state = useTraceStore.getState()
        expect(state.variables).toBeNull()
        expect(state.variablesLoading).toBe(false)
    })

    it('keeps the last stack when a background refresh fails', async () => {
        getStack.mockRejectedValue(new Error('refresh boom'))

        await useTraceStore.getState().refreshStack()

        expect(useTraceStore.getState().error).toBe('refresh boom')
    })

    it('reports a terminate failure through a notification', async () => {
        cancelTrace.mockRejectedValue(new Error('cannot terminate'))

        await useTraceStore.getState().terminate()

        // The status is left untouched when the cancel call fails.
        expect(cancelTrace).toHaveBeenCalledWith('p1')
    })

    it('surfaces a collect-watch failure and clears the spinner', async () => {
        cancelTrace.mockResolvedValue(undefined)
        startTrace.mockRejectedValue(new Error('collect boom'))

        await useTraceStore.getState().collectWatch()

        const state = useTraceStore.getState()
        expect(state.error).toBe('collect boom')
        expect(state.loading).toBe(false)
    })
})
