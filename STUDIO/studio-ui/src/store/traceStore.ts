import { create } from 'zustand'
import { notification } from 'antd'
import type {
    CallNodeView,
    DebugError,
    DebugFrameVariables,
    DebugFrameView,
    DebugStackView,
    DebugStatus,
    RawTableView,
    TraceParameterValue,
    WatchView,
} from 'types/trace'
import traceService from 'services/traceService'
import { isTraceExecutionTerminal } from 'utils/traceExecutionStatus'

/** Cap on rows fetched per table; the backend slices and reports totalRows when more rows exist. */
const MAX_TABLE_ROWS = 500

interface DebugState {
    // Route params
    projectId: string | null
    tableId: string | null
    fromModule: string | null
    testRanges: string | null
    inputJson: string | null

    // Session state
    status: DebugStatus | null
    frames: DebugFrameView[]
    /** The whole executed call tree once the trace finishes (profiling mode); shown instead of the empty stack. */
    tree: CallNodeView | null
    debugError: DebugError | null
    selectedFrameIndex: number | null
    variables: DebugFrameVariables | null
    variablesLoading: boolean
    /** Increments on every suspension so views that depend on the current line (table highlight) refresh. */
    stackVersion: number
    breakpoints: string[]
    breakpointLabels: Record<string, string>
    /** A one-shot breakpoint set by runTo; dropped on the next suspension so "run to here" leaves none behind. */
    transientBreakpoint: string | null
    /** Profiling mode: retain the executed call tree so returned branches stay browsable. Toggling restarts. */
    profiling: boolean
    /** Cells watched across the run, by name or ref. Applied on the next run, since a watch captures from the start. */
    watches: string[]
    /** The watched cells' values across the run, or null before they are fetched. */
    watch: WatchView | null
    /** Raw table grids cached by tableId for the session; the structure is immutable while suspended. */
    rawTableCache: Record<string, RawTableView>

    // UI
    loading: boolean
    error: string | null

    // Actions
    setRouteParams: (params: {
        projectId: string
        tableId: string
        fromModule?: string | null
        testRanges?: string | null
        inputJson?: string | null
    }) => void
    start: () => Promise<void>
    refreshStack: () => Promise<void>
    selectFrame: (index: number) => Promise<void>
    /** Load a table's raw grid, returning the cached copy when already fetched this session. */
    loadRawTable: (tableId: string) => Promise<RawTableView>
    stepInto: () => Promise<void>
    stepOver: () => Promise<void>
    stepOut: () => Promise<void>
    resume: () => Promise<void>
    pause: () => Promise<void>
    /** Run execution to a node (table/cell/rule breakpoint key) without leaving a permanent breakpoint. */
    runTo: (key: string, label?: string) => Promise<void>
    /** Turn profiling on/off; restarts the trace since the executed tree can only be captured from the start. */
    setProfiling: (value: boolean) => Promise<void>
    /** Replay a returned branch: restart from the top and run to that table so it is live again, with values. */
    replayNode: (uri: string, label?: string) => Promise<void>
    /** Replace the watch set. Applied on the next collect/run, since a watch captures from the start. */
    setWatchCells: (cells: string[]) => Promise<void>
    /** Run the whole trace to completion collecting the watched cells, then fetch the series. */
    collectWatch: () => Promise<void>
    /** Fetch the watched cells' values gathered so far. */
    fetchWatch: () => Promise<void>
    terminate: () => Promise<void>
    loadBreakpoints: () => Promise<void>
    toggleBreakpoint: (uri: string, label?: string) => Promise<void>
    onSocketStatus: (status: DebugStatus, message?: string) => void
    fetchTerminalError: () => Promise<void>
    fetchLazyParameter: (parameterId: number) => Promise<TraceParameterValue>
    reset: () => void
}

const initialState = {
    projectId: null,
    tableId: null,
    fromModule: null,
    testRanges: null,
    inputJson: null,
    status: null,
    frames: [],
    tree: null,
    debugError: null,
    selectedFrameIndex: null,
    variables: null,
    variablesLoading: false,
    stackVersion: 0,
    breakpoints: [],
    breakpointLabels: {},
    transientBreakpoint: null,
    profiling: false,
    watches: [],
    watch: null,
    rawTableCache: {},
    loading: false,
    error: null,
}

const isSuspended = (status: DebugStatus | null): boolean => status === 'suspended'

export const useTraceStore = create<DebugState>((set, get) => {
    /** Apply a freshly fetched stack, auto-selecting the current (top) frame when suspended. */
    const applyStack = (stack: DebugStackView): void => {
        const topIndex = stack.frames.length > 0 ? stack.frames.length - 1 : null
        const transient = get().transientBreakpoint
        set({
            status: stack.status,
            frames: stack.frames,
            tree: stack.tree ?? null,
            debugError: stack.error ?? null,
            selectedFrameIndex: isSuspended(stack.status) ? topIndex : null,
            variables: null,
            variablesLoading: false,
            stackVersion: get().stackVersion + 1,
            transientBreakpoint: null,
        })
        // Drop the one-shot run-to breakpoint once execution settles — whether it stopped there, stopped
        // at another breakpoint, or ran to the end without reaching it (a conditionally-skipped target).
        // applyStack only runs on a settled (non-running) stack, so clearing it here leaves none behind.
        // Remove it only if still present — a plain toggle would re-add a transient the user cleared meanwhile.
        if (transient && get().breakpoints.includes(transient)) {
            void get().toggleBreakpoint(transient)
        }
        if (isSuspended(stack.status) && topIndex !== null) {
            void get().selectFrame(topIndex)
        }
        // Watches accumulate as cells execute, so refresh the series on every stop (step/resume/completion),
        // not only on Collect — the panel then tracks the value as the user steps through.
        if (get().watches.length > 0) {
            void get().fetchWatch()
        }
    }

    const runStep = async (step: () => Promise<DebugStackView>): Promise<void> => {
        const { projectId } = get()
        if (!projectId) return
        set({ loading: true, error: null })
        try {
            applyStack(await step())
        } catch (error: any) {
            set({ error: error?.message || 'Step failed' })
        } finally {
            set({ loading: false })
        }
    }

    return {
        ...initialState,

        setRouteParams: ({ projectId, tableId, fromModule, testRanges, inputJson }) => {
            set({
                projectId,
                tableId,
                fromModule: fromModule ?? null,
                testRanges: testRanges ?? null,
                inputJson: inputJson ?? null,
            })
        },

        start: async () => {
            const { projectId, tableId, fromModule, testRanges, inputJson } = get()
            if (!projectId || !tableId) return
            set({ loading: true, error: null, status: 'pending' })
            try {
                // Attach to a session already created by the launcher; otherwise start a new one.
                let stack: DebugStackView
                try {
                    stack = await traceService.getStack(projectId)
                } catch {
                    stack = await traceService.startTrace(projectId, {
                        tableId,
                        ...(fromModule ? { fromModule } : {}),
                        ...(testRanges ? { testRanges } : {}),
                        ...(inputJson ? { inputJson } : {}),
                        stopAtEntry: true,
                        ...(get().profiling ? { profiling: true } : {}),
                    })
                }
                applyStack(stack)
            } catch (error: any) {
                set({ status: 'error', error: error?.message || 'Failed to start trace' })
            } finally {
                set({ loading: false })
            }
        },

        refreshStack: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                applyStack(await traceService.getStack(projectId))
            } catch (error: any) {
                set({ error: error?.message || 'Failed to load stack' })
            }
        },

        loadRawTable: async (tableId) => {
            const { projectId, rawTableCache } = get()
            if (!projectId) throw new Error('No project ID')
            const cached = rawTableCache[tableId]
            if (cached) return cached
            const raw = await traceService.getRawTable(projectId, tableId, MAX_TABLE_ROWS, true)
            set(s => ({ rawTableCache: { ...s.rawTableCache, [tableId]: raw } }))
            return raw
        },

        selectFrame: async (index) => {
            const { projectId, status, stackVersion } = get()
            if (!projectId) return
            set({ selectedFrameIndex: index })
            if (!isSuspended(status)) {
                set({ variables: null, variablesLoading: false })
                return
            }
            set({ variablesLoading: true, variables: null })
            // A slow variables response is stale if the user picked another frame or execution advanced
            // to a new suspension in the meantime; dropping it avoids showing one frame's data under another.
            const isStale = () => get().selectedFrameIndex !== index || get().stackVersion !== stackVersion
            try {
                const variables = await traceService.getVariables(projectId, index)
                if (isStale()) return
                set({ variables, variablesLoading: false })
            } catch (error: any) {
                if (isStale()) return
                notification.error({ title: error?.message || 'Failed to load variables' })
                set({ variables: null, variablesLoading: false })
            }
        },

        stepInto: () => runStep(() => traceService.step(get().projectId!, 'into')),
        stepOver: () => runStep(() => traceService.step(get().projectId!, 'over')),
        stepOut: () => runStep(() => traceService.step(get().projectId!, 'out')),

        resume: async () => {
            const { projectId } = get()
            if (!projectId) return
            set({ status: 'running', variables: null, variablesLoading: false })
            try {
                await traceService.resume(projectId)
            } catch (error: any) {
                set({ status: 'suspended', error: error?.message || 'Resume failed' })
            }
        },

        pause: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                await traceService.pause(projectId)
            } catch (error: any) {
                set({ error: error?.message || 'Pause failed' })
            }
        },

        runTo: async (key, label) => {
            // Run to a node (its breakpoint key) without leaving a permanent breakpoint: add a one-shot
            // breakpoint unless the user already pinned one here, then resume. applyStack drops it on the
            // next suspension. Only meaningful while paused.
            if (get().status !== 'suspended') return
            if (!get().breakpoints.includes(key)) {
                await get().toggleBreakpoint(key, label)
                set({ transientBreakpoint: key })
            }
            await get().resume()
        },

        setProfiling: async (value) => {
            const { projectId, profiling, breakpoints } = get()
            if (!projectId || profiling === value) return
            // The executed tree can only be captured from the start, so switching restarts the session.
            set({ profiling: value })
            await get().terminate()
            await get().start()
            // A fresh session has no breakpoints; re-apply the ones the user had set.
            if (breakpoints.length > 0) {
                try {
                    await traceService.setBreakpoints(projectId, breakpoints)
                } catch {
                    // best-effort: the user can re-add them
                }
            }
        },

        replayNode: async (uri, label) => {
            if (!get().projectId) return
            // The executed tree has structure only, so to inspect a returned branch we re-run to it: restart
            // from the top, then run to that table, where it becomes the live frame again with its values.
            await get().terminate()
            await get().start()
            await get().runTo(uri, label)
        },

        setWatchCells: async (cells) => {
            const { projectId, watch } = get()
            // Drop any already-collected series for cells that are no longer watched, so removing a watch
            // clears its values instead of leaving stale rows on screen.
            const series = watch ? watch.series.filter(s => cells.includes(s.name)) : []
            set({ watches: cells, watch: watch ? { ...watch, series } : null })
            if (!projectId) return
            // Applies to the running session immediately (like breakpoints), so a cell added mid-debug is
            // captured as stepping reaches it; on the next start it captures from the beginning of the run.
            await traceService.setWatches(projectId, cells)
            if (cells.length > 0) await get().fetchWatch()
        },

        collectWatch: async () => {
            const { projectId, tableId, fromModule, testRanges, inputJson } = get()
            if (!projectId || !tableId) return
            set({ loading: true, error: null })
            try {
                // Run the whole trace to completion (not the full tree) so every execution is captured.
                await get().terminate()
                const stack = await traceService.startTrace(projectId, {
                    tableId,
                    ...(fromModule ? { fromModule } : {}),
                    ...(testRanges ? { testRanges } : {}),
                    ...(inputJson ? { inputJson } : {}),
                    stopAtEntry: false,
                    includeTree: false,
                })
                applyStack(stack)
                await get().fetchWatch()
            } catch (error: any) {
                set({ error: error?.message || 'Failed to collect watches' })
            } finally {
                set({ loading: false })
            }
        },

        fetchWatch: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                set({ watch: await traceService.getWatch(projectId) })
            } catch {
                // Best effort: the panel keeps whatever it last showed.
            }
        },

        terminate: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                await traceService.cancelTrace(projectId)
                set({ status: 'terminated', frames: [], selectedFrameIndex: null, variables: null, variablesLoading: false })
            } catch (error: any) {
                notification.error({ title: error?.message || 'Failed to terminate' })
            }
        },

        loadBreakpoints: async () => {
            const { projectId, breakpointLabels } = get()
            if (!projectId) return
            try {
                const uris = await traceService.getBreakpoints(projectId)
                const labels = { ...breakpointLabels }
                uris.forEach(uri => {
                    if (!labels[uri]) labels[uri] = uri.substring(uri.lastIndexOf('/') + 1) || uri
                })
                set({ breakpoints: uris, breakpointLabels: labels })
            } catch {
                // breakpoints are best-effort
            }
        },

        toggleBreakpoint: async (uri, label) => {
            const { projectId, breakpoints, breakpointLabels } = get()
            if (!projectId) return
            const has = breakpoints.includes(uri)
            const next = has ? breakpoints.filter(b => b !== uri) : [...breakpoints, uri]
            const labels = { ...breakpointLabels }
            if (has) {
                delete labels[uri]
            } else {
                labels[uri] = label || uri.substring(uri.lastIndexOf('/') + 1) || uri
            }
            set({ breakpoints: next, breakpointLabels: labels })
            try {
                await traceService.setBreakpoints(projectId, next)
            } catch (error: any) {
                notification.error({ title: error?.message || 'Failed to update breakpoints' })
                set({ breakpoints, breakpointLabels })
            }
        },

        onSocketStatus: (status, message) => {
            if (status === 'suspended') {
                // A synchronous step applies the authoritative stack from its own response; the WS
                // notification for that same suspension would only trigger a duplicate stack+variables fetch.
                if (get().loading) return
                void get().refreshStack()
            } else if (status === 'running') {
                set({ status: 'running' })
            } else if (isTraceExecutionTerminal(status)) {
                // Show an immediate summary from the socket (if any); the full error is fetched below.
                set({
                    status,
                    frames: [],
                    selectedFrameIndex: null,
                    variables: null,
                    variablesLoading: false,
                    debugError: status === 'error' && message ? { summary: message } : null,
                })
                if (status === 'error') {
                    void get().fetchTerminalError()
                }
            }
        },

        fetchTerminalError: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                // The session is still readable after it errors; the stack carries the cleaned, located error.
                const stack = await traceService.getStack(projectId)
                if (stack.error) {
                    set({ debugError: stack.error })
                }
            } catch {
                // Best effort: keep the socket summary if the fetch fails.
            }
        },

        fetchLazyParameter: async (parameterId) => {
            const { projectId, variables } = get()
            if (!projectId) throw new Error('No project ID')
            const result = await traceService.getParameterValue(projectId, parameterId)
            if (variables) {
                const patch = (p?: TraceParameterValue | null): TraceParameterValue | null =>
                    (p && p.parameterId === parameterId ? { ...p, value: result.value } : p) ?? null
                set({
                    variables: {
                        ...variables,
                        parameters: variables.parameters.map(p =>
                            p.parameterId === parameterId ? { ...p, value: result.value } : p),
                        context: patch(variables.context),
                        result: patch(variables.result),
                    },
                })
            }
            return result
        },

        reset: () => set(initialState),
    }
})
