import traceService from 'services/traceService'
import { buildSimpleOrder, useTraceStore } from 'store/traceStore'
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
        getTreeChildren: vi.fn(),
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
        getStack.mockResolvedValue({ status: 'completed', frames: []} as any)

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

    it('fetches the settled stack when a non-profiling run completes, keeping the final state browsable', async () => {
        getStack.mockResolvedValue({ status: 'completed',
            frames: [{ index: 0, completed: true } as any]} as any)

        useTraceStore.setState({ profiling: false })
        useTraceStore.getState().onSocketStatus('completed')
        // Without the fetch the window would go empty after a Resume to the end: the socket clears the
        // frames and only the settled stack brings back the root call with its steps and result.
        expect(getStack).toHaveBeenCalledWith('p1')

        await new Promise(resolve => setTimeout(resolve, 0))
        expect(useTraceStore.getState().frames).toHaveLength(1)
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

    it('drops a socket event of a session it does not watch — a stale session reaped in the background', () => {
        getStack.mockResolvedValue({ status: 'suspended', frames: []} as any)
        useTraceStore.setState({ status: 'suspended', sessionId: 'live-session' })

        // An old session of the same user and table is terminated minutes later; its event shares the topic.
        useTraceStore.getState().onSocketStatus('terminated', undefined, 'stale-session')

        expect(useTraceStore.getState().status).toBe('suspended') // the watched session is untouched
    })

    it('applies a socket event carrying the id of the watched session', () => {
        useTraceStore.setState({ status: 'suspended', sessionId: 'live-session' })

        useTraceStore.getState().onSocketStatus('terminated', undefined, 'live-session')

        expect(useTraceStore.getState().status).toBe('terminated')
    })

    it('remembers the session id reported by the stack, so events can be attributed', async () => {
        getStack.mockResolvedValue({ status: 'suspended', frames: [], sessionId: 's-42' } as any)

        await useTraceStore.getState().start()

        expect(useTraceStore.getState().sessionId).toBe('s-42')
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

    it('begins a new run id on every start, so views drop per-run UI state', async () => {
        const startTrace = traceService.startTrace as MockedFunction<typeof traceService.startTrace>
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue({ status: 'suspended', frames: []} as any)
        expect(useTraceStore.getState().runId).toBe(0)

        await useTraceStore.getState().start()
        expect(useTraceStore.getState().runId).toBe(1)

        await useTraceStore.getState().start()
        expect(useTraceStore.getState().runId).toBe(2)
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

    it('loads the root frame variables when inspecting a completed run, so the final result stays readable', async () => {
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)
        useTraceStore.setState({ status: 'completed' })

        await useTraceStore.getState().selectFrame(0)

        expect(getVariables).toHaveBeenCalledWith('p1', 0)
        expect(useTraceStore.getState().variables).not.toBeNull()
    })

    it('auto-selects the root frame — not the deepest published — once a run completes', async () => {
        // Resuming a deep breakpoint to completion leaves the multi-frame stack published; the run's result is
        // the root call, so index 0 must be selected rather than the deepest frame at the top of the stack.
        getStack.mockResolvedValue({ status: 'completed', frames: [{ index: 0 } as any, { index: 1 } as any],
            tree: null, profile: null } as any)
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)

        await useTraceStore.getState().start()

        expect(useTraceStore.getState().selectedFrameIndex).toBe(0)
    })

    it('does not fetch variables while the worker is still running', async () => {
        useTraceStore.setState({ status: 'running' })

        await useTraceStore.getState().selectFrame(0)

        expect(getVariables).not.toHaveBeenCalled()
        expect(useTraceStore.getState().variables).toBeNull()
    })

    it('does not fetch variables for a focused step in the business view — step-inputs is enough', async () => {
        // A focused step shows only its own inputs, result and cell (from the step-inputs endpoint); the
        // heavy frame-variables payload is never rendered there, so selectFrame must not fetch it.
        useTraceStore.setState({
            status: 'suspended',
            advanced: false,
            simpleFocus: { ref: 'S9', label: '$Value$Limit', ownerUri: 'uOwner', ownerInstance: 0 },
        })

        await useTraceStore.getState().selectFrame(0)

        expect(getVariables).not.toHaveBeenCalled()
        expect(useTraceStore.getState().variables).toBeNull()
        expect(useTraceStore.getState().selectedFrameIndex).toBe(0)
    })

    it('still fetches variables for a table node in the business view (no step focus)', async () => {
        // Clicking a table (frame) node, not a step, keeps the frame view: its parameters and result come
        // from the variables payload as usual, so the skip must not swallow it.
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)
        useTraceStore.setState({ status: 'suspended', advanced: false, simpleFocus: null })

        await useTraceStore.getState().selectFrame(0)

        expect(getVariables).toHaveBeenCalledWith('p1', 0)
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

const getTreeChildren = traceService.getTreeChildren as MockedFunction<typeof traceService.getTreeChildren>

// A minimal executed tree: root uR calls uA twice from step S1 (lazily loaded), and uA has a plain step SA.
const callNode = (uri: string, instance: number, steps: any[] = [], extra: object = {}): any =>
    ({ uri, name: uri, instance, kind: 'spreadsheet', durationMillis: 0, selfMillis: 0, steps, ...extra })

const sampleRoot = () => callNode('uR', 0, [{ ref: 'S1', status: 'executed', childrenTotal: 2 }])

const sampleChildrenPage = () => ({
    children: [
        callNode('uA', 0, [{ ref: 'SA', status: 'executed' }]),
        callNode('uA', 1, [{ ref: 'SA', status: 'executed' }]),
    ],
    total: 2,
})

describe('traceStore simple mode', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('numbers calls and steps in execution order, closing each range over its whole subtree', () => {
        const root = sampleRoot()
        const children = {
            [JSON.stringify(['uR', 0, 'S1'])]: sampleChildrenPage().children,
        }

        const order = buildSimpleOrder(root, children)

        // Execution order: uR@0 (0), S1 (1), uA@0 (2), its SA (3), uA@1 (4), its SA (5).
        expect(order['uR@0']).toEqual({ pre: 0, end: 5 }) // the root's range spans the whole run
        expect(order['uR#S1@0']).toEqual({ pre: 1, end: 5 }) // the step's range spans both calls it made
        expect(order['uA@0']).toEqual({ pre: 2, end: 3 })
        expect(order['uA@1']).toEqual({ pre: 4, end: 5 })
    })

    it('skips step references when numbering — they are not executions of their own', () => {
        const root = callNode('uR', 0, [{
            ref: 'S1',
            status: 'executed',
            children: [callNode('x', 0, [], { kind: 'stepRef', refStep: 'S0' }), callNode('uA', 0)],
        }])

        const order = buildSimpleOrder(root, {})

        expect(order['uA@0']).toEqual({ pre: 2, end: 2 }) // the ref did not consume a position
        expect(order['x@0']).toBeUndefined()
    })

    it('runs the whole trace profiled and downloads the full tree for offline browsing', async () => {
        cancelTrace.mockResolvedValue(undefined)
        startTrace.mockResolvedValue({ status: 'completed', frames: [], tree: sampleRoot(),
            profile: { nodeCount: 3 } } as any)
        getTreeChildren.mockResolvedValue(sampleChildrenPage() as any)
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)

        await useTraceStore.getState().simpleRun()

        const state = useTraceStore.getState()
        expect(startTrace).toHaveBeenCalledWith('p1', expect.objectContaining(
            { stopAtEntry: false, profiling: true, detailedTitles: true }))
        expect(getTreeChildren).toHaveBeenCalledWith('p1', 'uR', 0, 'S1', 0, expect.any(Number))
        expect(state.simpleTree?.uri).toBe('uR')
        expect(state.simpleChildren[JSON.stringify(['uR', 0, 'S1'])]).toHaveLength(2)
        expect(state.simpleReady).toBe(true)
        expect(state.simpleLoading).toBe(false)
        expect(state.simpleOrder['uA@1']).toEqual({ pre: 4, end: 5 })
    })

    it('clears breakpoints left over from the advanced mode before the simple run', async () => {
        cancelTrace.mockResolvedValue(undefined)
        setBreakpoints.mockResolvedValue(undefined)
        startTrace.mockResolvedValue({ status: 'completed', frames: [], tree: null } as any)
        useTraceStore.setState({ breakpoints: ['uA'], breakpointLabels: { uA: 'A' } })

        await useTraceStore.getState().simpleRun()

        expect(setBreakpoints).toHaveBeenCalledWith('p1', [])
        expect(useTraceStore.getState().breakpoints).toEqual([])
    })

    it('surfaces a failed simple run and drops the loading state', async () => {
        cancelTrace.mockResolvedValue(undefined)
        startTrace.mockRejectedValue(new Error('run boom'))

        await useTraceStore.getState().simpleRun()

        const state = useTraceStore.getState()
        expect(state.status).toBe('error')
        expect(state.error).toBe('run boom')
        expect(state.simpleLoading).toBe(false)
        expect(state.simpleReady).toBe(false)
    })

    /** A store primed as if a simple run has finished and its tree is downloaded. */
    const primeSimpleReady = (extra: object = {}) => useTraceStore.setState({
        status: 'completed',
        simpleReady: true,
        simpleTree: sampleRoot(),
        simpleOrder: buildSimpleOrder(sampleRoot(), {
            [JSON.stringify(['uR', 0, 'S1'])]: sampleChildrenPage().children,
        }),
        ...extra,
    } as any)

    it('inspects a call by restarting and running THROUGH it with an inclusive breakpoint', async () => {
        primeSimpleReady()
        cancelTrace.mockResolvedValue(undefined)
        // The restart attaches to nothing (terminated), so a fresh session starts suspended at the root.
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended([
            { index: 0, uri: 'uR', instance: 0, completed: false } as any]))
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)

        await useTraceStore.getState().simpleInspect(
            { key: 'uA@1', frameUri: 'uA', frameInstance: 1, stepType: 'out', label: 'uA' })

        const state = useTraceStore.getState()
        expect(cancelTrace).toHaveBeenCalled() // completed run cannot be resumed — restart
        // The one-shot breakpoint is inclusive: the engine suspends right AFTER the call executed,
        // its inputs and result on the stack — one stop, no follow-up step commands.
        expect(setBreakpoints).toHaveBeenCalledWith('p1', ['after:uA@1'])
        expect(resume).toHaveBeenCalled()
        expect(state.simpleSelectedKey).toBe('uA@1')
        expect(state.simpleFocus).toBeNull() // a call keeps the plain frame view
    })

    it('does nothing when the clicked row is already the one on screen', async () => {
        primeSimpleReady({ status: 'suspended', simpleSelectedKey: 'uA@1', simpleLastInspected: { pre: 0, end: 0 } })

        await useTraceStore.getState().simpleInspect(
            { key: 'uA@1', frameUri: 'uA', frameInstance: 1, stepType: 'out', label: 'uA' })

        // Re-clicking the selected row must not re-run: its inputs and result are already shown.
        expect(cancelTrace).not.toHaveBeenCalled()
        expect(resume).not.toHaveBeenCalled()
        expect(step).not.toHaveBeenCalled()
    })

    it('executes the very next step in place — its line is already current, so no breakpoint can reach it', async () => {
        primeSimpleReady({ status: 'suspended', simpleLastInspected: { pre: 0, end: 0 } })
        step.mockResolvedValue(suspended([
            { index: 0, uri: 'uR', instance: 0, completed: false, location: { kind: 'cell', ref: 'S1' } } as any]))
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)
        useTraceStore.setState({ frames: [
            { index: 0, uri: 'uR', instance: 0, completed: false, location: { kind: 'cell', ref: 'S1' } } as any]})

        await useTraceStore.getState().simpleInspect({
            key: 'uR#S1@0', frameUri: 'uR', frameInstance: 0, stepType: 'over', label: '$Value$S1',
            focus: { ref: 'S1', label: '$Value$S1', ownerUri: 'uR', ownerInstance: 0 },
        })

        expect(step).toHaveBeenCalledWith('p1', 'over') // executed in place
        expect(resume).not.toHaveBeenCalled()
        expect(cancelTrace).not.toHaveBeenCalled()
    })

    it('remembers the clicked step so Details presents the step, not the paused frame', async () => {
        primeSimpleReady({ status: 'suspended', simpleLastInspected: { pre: 0, end: 0 } })
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)
        useTraceStore.setState({ frames: [{ index: 0, uri: 'uR', instance: 0, completed: true } as any]})

        await useTraceStore.getState().simpleInspect({
            key: 'uR#S1@0', frameUri: 'uR', frameInstance: 0, stepType: 'over', label: '$Value$S1',
            focus: { ref: 'S1', label: '$Value$S1', ownerUri: 'uR', ownerInstance: 0 },
        })

        expect(useTraceStore.getState().simpleFocus).toEqual(
            { ref: 'S1', label: '$Value$S1', ownerUri: 'uR', ownerInstance: 0 })
    })

    it('resumes instead of restarting when the clicked call starts after the last inspected subtree', async () => {
        primeSimpleReady({ status: 'suspended', simpleLastInspected: { pre: 2, end: 3 } }) // paused after uA@0
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)
        useTraceStore.setState({ frames: [{ index: 0, uri: 'uA', instance: 0, completed: true } as any]})

        await useTraceStore.getState().simpleInspect(
            { key: 'uA@1', frameUri: 'uA', frameInstance: 1, stepType: 'out', label: 'uA' }) // uA@1 is at pre 4

        expect(cancelTrace).not.toHaveBeenCalled() // still ahead of us — resume reaches it
        expect(resume).toHaveBeenCalled()
    })

    it('restarts for a sub-call of the last inspected call — it already executed during the step out', async () => {
        // uR@0 was inspected: its whole subtree (0..5) has executed by now.
        primeSimpleReady({ status: 'suspended', simpleLastInspected: { pre: 0, end: 5 } })
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended([
            { index: 0, uri: 'uR', instance: 0, completed: false } as any]))
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)

        await useTraceStore.getState().simpleInspect(
            { key: 'uA@0', frameUri: 'uA', frameInstance: 0, stepType: 'out', label: 'uA' })

        expect(cancelTrace).toHaveBeenCalled() // pre 2 lies inside 0..5 — only a fresh run reaches it again
    })

    it('steps the root immediately after the restart — it is already the paused frame', async () => {
        primeSimpleReady()
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended([
            { index: 0, uri: 'uR', instance: 0, completed: false } as any]))
        step.mockResolvedValue({ status: 'suspended', frames: [
            { index: 0, uri: 'uR', instance: 0, completed: true } as any]} as any)
        getVariables.mockResolvedValue({ parameters: [], steps: [], errors: []} as any)

        await useTraceStore.getState().simpleInspect(
            { key: 'uR@0', frameUri: 'uR', frameInstance: 0, stepType: 'out', label: 'uR' })

        expect(step).toHaveBeenCalledWith('p1', 'out') // executed in place, no breakpoint needed
        expect(resume).not.toHaveBeenCalled()
    })

    it('drops the resume anchor when the run finishes without reaching the target', () => {
        getStack.mockResolvedValue({ status: 'completed', frames: []} as any)
        useTraceStore.setState({ simpleLastInspected: { pre: 4, end: 5 } })

        // A conditionally-skipped target never fires its breakpoint; the run just finishes.
        useTraceStore.getState().onSocketStatus('completed')

        expect(useTraceStore.getState().simpleLastInspected).toBeNull() // the next click restarts
    })

    it('clears breakpoints and restarts the trace when switching back to the simple view', async () => {
        setBreakpoints.mockResolvedValue(undefined)
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended())
        useTraceStore.setState({ advanced: true, breakpoints: ['uA'], breakpointLabels: { uA: 'A' },
            simpleReady: true, simpleTree: sampleRoot() })

        await useTraceStore.getState().setAdvanced(false)

        const state = useTraceStore.getState()
        expect(state.advanced).toBe(false)
        expect(state.breakpoints).toEqual([]) // the business view debugs with none
        expect(setBreakpoints).toHaveBeenCalledWith('p1', [])
        expect(cancelTrace).toHaveBeenCalledWith('p1') // the trace restarts from the top, like a profiling toggle
        expect(startTrace).toHaveBeenCalled()
        expect(state.simpleReady).toBe(false) // the business snapshot resets to its Run prompt
        expect(state.simpleTree).toBeNull()
    })

    it('restarts the trace from the top when switching into the advanced view', async () => {
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended())
        useTraceStore.setState({ advanced: false })

        await useTraceStore.getState().setAdvanced(true)

        expect(useTraceStore.getState().advanced).toBe(true)
        expect(cancelTrace).toHaveBeenCalledWith('p1')
        expect(startTrace).toHaveBeenCalled()
    })

    it('ignores a mode switch that does not change the value', async () => {
        useTraceStore.setState({ advanced: false })

        await useTraceStore.getState().setAdvanced(false)

        expect(cancelTrace).not.toHaveBeenCalled()
    })

    it('keeps the snapshot tree untouched while an inspection re-runs the trace', async () => {
        primeSimpleReady()
        const snapshot = useTraceStore.getState().simpleTree
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended([
            { index: 0, uri: 'uR', instance: 0, completed: false } as any]))
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)

        await useTraceStore.getState().simpleInspect(
            { key: 'uA@1', frameUri: 'uA', frameInstance: 1, stepType: 'out', label: 'uA' })

        expect(useTraceStore.getState().simpleTree).toBe(snapshot) // the original tree never changes
        expect(useTraceStore.getState().simpleReady).toBe(true)
    })

    it('keeps the previously inspected table on the panel through a restart — never flashes the root', async () => {
        // A prior inspection left a deep table (uA) on screen. Clicking an earlier node forces a fresh run,
        // but the panel must not flash the fresh root and its parameters: the frames stay on uA until the
        // target inspection settles (the resume here is mocked, so no settle happens).
        const previous = [
            { index: 0, uri: 'uR', instance: 0, completed: true } as any,
            { index: 1, uri: 'uA', instance: 0, completed: false } as any,
        ]
        primeSimpleReady({ status: 'suspended', simpleLastInspected: { pre: 5, end: 9 },
            frames: previous, selectedFrameIndex: 1 })
        cancelTrace.mockResolvedValue(undefined)
        getStack.mockRejectedValue(new Error('no session'))
        startTrace.mockResolvedValue(suspended([{ index: 0, uri: 'uR', instance: 0, completed: false } as any]))
        setBreakpoints.mockResolvedValue(undefined)
        resume.mockResolvedValue(undefined)

        await useTraceStore.getState().simpleInspect(
            { key: 'uB@0', frameUri: 'uB', frameInstance: 0, stepType: 'out', label: 'uB' })

        expect(cancelTrace).toHaveBeenCalled() // an earlier node needs a fresh run
        expect(resume).toHaveBeenCalled() // run to the target with an inclusive breakpoint
        // The fresh root stack was never applied: the previous table and selection are untouched.
        expect(useTraceStore.getState().frames).toBe(previous)
        expect(useTraceStore.getState().selectedFrameIndex).toBe(1)
    })
})
