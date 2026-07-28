import { create } from 'zustand'
import { notification } from 'antd'
import type {
    CallNodeView,
    DebugError,
    DebugFrameVariables,
    DebugFrameView,
    DebugStackView,
    DebugStatus,
    ProfileSummaryView,
    RawTableView,
    StepType,
    StepValueView,
    TraceParameterValue,
    WatchView,
} from 'types/trace'
import traceService from 'services/traceService'
import { isTraceExecutionTerminal } from 'utils/traceExecutionStatus'

/** Cap on rows fetched per table; the backend slices and reports totalRows when more rows exist. */
const MAX_TABLE_ROWS = 500

/** The `startTrace` request shape, derived from the service so it stays in sync without duplication. */
type LaunchOptions = Parameters<typeof traceService.startTrace>[1]

/**
 * Base launch options shared by every start: the table plus the current module / test-range / input scoping.
 * Callers add the mode-specific flags (stopAtEntry, profiling, includeTree) as overrides.
 */
const launchOptions = (
    scope: { tableId: string; fromModule: string | null; testRanges: string | null; inputJson: string | null },
    overrides: Partial<LaunchOptions>
): LaunchOptions => ({
    tableId: scope.tableId,
    ...(scope.fromModule ? { fromModule: scope.fromModule } : {}),
    ...(scope.testRanges ? { testRanges: scope.testRanges } : {}),
    ...(scope.inputJson ? { inputJson: scope.inputJson } : {}),
    ...overrides,
})

/** Execution-order range of a call or step in the simple-mode tree: where it starts and where its subtree ends. */
export interface SimpleOrderRange {
    pre: number
    end: number
}

/**
 * A clicked step, remembered so the Details panel can present the suspension as that step — like the
 * classic trace did: the owning table's inputs as Parameters, the step's own value as `return`.
 */
export interface SimpleStepFocus {
    /** Step reference within its owning table (`R2C3`-style). */
    ref: string
    /** Display name of the step (its `$...` cell name). */
    label: string
    /** The owning table, to locate its frame on the suspended stack. */
    ownerUri: string
    ownerInstance: number
}

/** A tree row the simple mode inspects: its breakpoint key plus the frame the suspension lands in. */
export interface SimpleInspectTarget {
    /** Breakpoint key: `uri@instance` for a call, `uri#ref@instance` for a step. */
    key: string
    /** URI of the frame the suspension stops in (the call itself, or the step's owning table). */
    frameUri: string
    /** Execution index of that frame. */
    frameInstance: number
    /** The step that completes the target after the stop: `out` runs a call to its exit, `over` runs a step. */
    stepType: Extract<StepType, 'out' | 'over'>
    /** Display label for the one-shot breakpoint. */
    label?: string
    /** Set when the clicked row is a step, so Details shows the step rather than the paused frame. */
    focus?: SimpleStepFocus
    /**
     * Unique identity of the clicked tree row, for the selection highlight. Distinct from {@link key}:
     * several rows can share one run key — every static cell of a table runs the same table through — so
     * the highlight must key off the row itself, not the run target, or all of them would light up.
     */
    selectionKey?: string
}

interface DebugState {
    // Route params
    projectId: string | null
    tableId: string | null
    fromModule: string | null
    testRanges: string | null
    inputJson: string | null

    // Session state
    status: DebugStatus | null
    /** Identity of the watched debug session; socket events of any other session are dropped. */
    sessionId: string | null
    frames: DebugFrameView[]
    /** The whole executed call tree once the trace finishes (profiling mode); shown instead of the empty stack. */
    tree: CallNodeView | null
    /** Lazily-fetched children of expanded tree steps, keyed by `treeChildKey(uri, instance, step)`; paged. */
    treeChildren: Record<string, CallNodeView[]>
    /** Tree steps currently fetching their next page of children, so the row can show a spinner. */
    treeLoading: Record<string, boolean>
    /** Bounded hot-spots overview of a finished profiling run (slowest tables by own time); null otherwise. */
    profile: ProfileSummaryView | null
    debugError: DebugError | null
    selectedFrameIndex: number | null
    variables: DebugFrameVariables | null
    variablesLoading: boolean
    /** Increments on every suspension so views that depend on the current line (table highlight) refresh. */
    stackVersion: number
    /** Increments each time a new run starts, so views can drop per-run UI state (tree expansions). */
    runId: number
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

    // Simple (business) mode: a full profiled run downloaded whole, browsed offline, inspected by re-running.
    /** Whether the advanced debugger UI is shown; the simple business view is the default. */
    advanced: boolean
    /** The executed call tree of the last simple-mode run. Immutable once captured — re-runs never touch it. */
    simpleTree: CallNodeView | null
    /** Every step's sub-calls of that tree, downloaded up front so expanding never calls the backend. */
    simpleChildren: Record<string, CallNodeView[]>
    /** Execution-order ranges by breakpoint key, deciding whether a click can resume or must restart. */
    simpleOrder: Record<string, SimpleOrderRange>
    /** True once the whole tree is downloaded and browsable. */
    simpleReady: boolean
    /** True while the simple run executes and its tree downloads. */
    simpleLoading: boolean
    /** Calls downloaded so far, for the preparation progress line. */
    simpleLoadedCount: number
    /** Full call count of the run (from the profile), or null when unknown. */
    simpleTotalCount: number | null
    /** Breakpoint key of the row whose values are being shown, for the selection highlight. */
    simpleSelectedKey: string | null
    /** Order range of the last inspected row; a later row can be resumed to, an earlier one needs a restart. */
    simpleLastInspected: SimpleOrderRange | null
    /** The clicked step when the last inspection was a step, so Details presents the step, not the frame. */
    simpleFocus: SimpleStepFocus | null

    /** Show the decision-table per-condition breakdown in both trees; off shows only the returned rule. */
    showDetailed: boolean

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
    /** Re-run the whole trace from the top with the current table, input, profiling, and watches. */
    rerun: () => Promise<void>
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
    onSocketStatus: (status: DebugStatus, message?: string, sessionId?: string | null) => void
    fetchTerminalError: () => Promise<void>
    fetchLazyParameter: (parameterId: number) => Promise<TraceParameterValue>
    /** Fetch the next page of a tree step's executed sub-calls (lazy executed-tree loading). */
    fetchTreeChildren: (uri: string, instance: number, step: string) => Promise<void>
    /**
     * Switch between the simple business view and the advanced debugger. Entering simple clears breakpoints,
     * and either direction restarts the trace from the top so neither view inherits the other's position.
     */
    setAdvanced: (value: boolean) => Promise<void>
    /** Toggle the decision-table condition breakdown shown in both trees (business and advanced). */
    setShowDetailed: (value: boolean) => void
    /** Simple mode Run: execute the whole trace recording its tree, then download the tree for offline browsing. */
    simpleRun: () => Promise<void>
    /** Simple mode click: re-run execution through the clicked row so its inputs and result become readable. */
    simpleInspect: (target: SimpleInspectTarget) => Promise<void>
    reset: () => void
}

/** Cache key for a tree step's lazily-fetched children: the frame's (uri, instance) plus the step ref. */
export const treeChildKey = (uri: string, instance: number, step: string): string =>
    JSON.stringify([uri, instance, step])

/**
 * Number every call and step of a fully downloaded tree in execution order. `pre` is the position where
 * the row starts executing and `end` the last position inside it, so a click can only be reached by
 * resuming when its `pre` lies after the whole subtree of the previously inspected row — anything at or
 * before that point (including the row's own sub-calls) has already executed and needs a restart.
 */
export const buildSimpleOrder = (
    root: CallNodeView,
    children: Record<string, CallNodeView[]>
): Record<string, SimpleOrderRange> => {
    const order: Record<string, SimpleOrderRange> = {}
    let counter = 0
    const visit = (node: CallNodeView): void => {
        // A step reference is not an execution of its own — the original step already holds its range.
        if (node.kind === 'stepRef') {
            return
        }
        const pre = counter
        counter += 1
        for (const step of node.steps) {
            const stepPre = counter
            counter += 1
            const kids = step.children ?? children[treeChildKey(node.uri, node.instance, step.ref)] ?? []
            kids.forEach(visit)
            order[`${node.uri}#${step.ref}@${node.instance}`] = { pre: stepPre, end: counter - 1 }
        }
        order[`${node.uri}@${node.instance}`] = { pre, end: counter - 1 }
    }
    visit(root)
    return order
}

const initialState = {
    projectId: null,
    tableId: null,
    fromModule: null,
    testRanges: null,
    inputJson: null,
    status: null,
    sessionId: null,
    frames: [],
    tree: null,
    treeChildren: {},
    treeLoading: {},
    profile: null,
    debugError: null,
    selectedFrameIndex: null,
    variables: null,
    variablesLoading: false,
    stackVersion: 0,
    runId: 0,
    breakpoints: [],
    breakpointLabels: {},
    transientBreakpoint: null,
    profiling: false,
    watches: [],
    watch: null,
    rawTableCache: {},
    advanced: false,
    simpleTree: null,
    simpleChildren: {},
    simpleOrder: {},
    simpleReady: false,
    simpleLoading: false,
    simpleLoadedCount: 0,
    simpleTotalCount: null,
    simpleSelectedKey: null,
    simpleLastInspected: null,
    simpleFocus: null,
    showDetailed: false,
    loading: false,
    error: null,
}

/** The business-mode snapshot fields at rest, so a fresh run or a view switch returns the view to its Run prompt. */
const SIMPLE_SNAPSHOT_RESET = {
    simpleTree: null,
    simpleChildren: {},
    simpleOrder: {},
    simpleReady: false,
    simpleLoading: false,
    simpleLoadedCount: 0,
    simpleTotalCount: null,
    simpleSelectedKey: null,
    simpleLastInspected: null,
    simpleFocus: null,
}

// A settled run whose frames can still be inspected: paused at a step, or finished (completed/failed) with
// the root frame still published — so the final result is readable after a run to completion, not only at a stop.
const isInspectable = (status: DebugStatus | null): boolean =>
    status === 'suspended' || status === 'completed' || status === 'error'

/** Sub-calls requested per lazy /tree/children page; the server caps a page at this size too. */
const TREE_PAGE_SIZE = 100

/** Sub-calls per page while downloading the whole tree up front (simple mode) — large pages, few requests. */
const SIMPLE_FETCH_PAGE = 500

/** Parallel page downloads while the simple mode pulls the whole tree. */
const SIMPLE_FETCH_CONCURRENCY = 4

export const useTraceStore = create<DebugState>((set, get) => {
    // True only while a quiet inspect restart cancels the old session and starts a fresh one, so the old
    // session's 'terminated' socket echo can be ignored instead of blanking the panel mid-restart.
    let restarting = false

    /** Apply a freshly fetched stack, auto-selecting the current (top) frame when suspended. */
    const applyStack = (stack: DebugStackView): void => {
        const topIndex = stack.frames.length > 0 ? stack.frames.length - 1 : null
        // While suspended (or stopped at an error) the frame of interest is the current/failing one at the top;
        // once the run completes, the result to surface is the root call at index 0 — not whichever deep frame
        // the last suspend happened to leave published.
        const focusIndex = !isInspectable(stack.status) || topIndex === null ? null
            : stack.status === 'completed' ? 0
                : topIndex
        const transient = get().transientBreakpoint
        set({
            status: stack.status,
            // Every stack response names its session; remembering it lets socket events be attributed.
            ...(stack.sessionId ? { sessionId: stack.sessionId } : {}),
            frames: stack.frames,
            tree: stack.tree ?? null,
            // A run without a tree yet (starting/running/rerun) invalidates any browsed sub-calls from the
            // previous run; keep them only while browsing the completed tree.
            ...(stack.tree ? {} : { treeChildren: {}, treeLoading: {} }),
            profile: stack.profile ?? null,
            debugError: stack.error ?? null,
            selectedFrameIndex: focusIndex,
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
        if (focusIndex !== null) {
            void get().selectFrame(focusIndex)
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

    /**
     * Mark the beginning of a new run: clear transient flags and bump runId so views drop their
     * per-run UI state (tree expansions). Every path that launches a run must go through this.
     */
    const beginRun = (extra: Partial<DebugState> = {}): void =>
        set({ loading: true, error: null, runId: get().runId + 1, ...extra })

    /**
     * Download the entire executed tree: every step's sub-calls, breadth-first, so the simple view can
     * expand any branch instantly without further backend calls. Returns null when the download fails or
     * a newer run supersedes it (the token no longer matches the current runId).
     */
    const downloadTree = async (
        projectId: string,
        root: CallNodeView,
        token: number
    ): Promise<Record<string, CallNodeView[]> | null> => {
        const children: Record<string, CallNodeView[]> = {}
        const tasks: { uri: string; instance: number; step: StepValueView }[] = []
        let loaded = 0
        const enqueue = (node: CallNodeView): void => {
            loaded += 1
            for (const step of node.steps) {
                if (step.children) {
                    step.children.forEach(enqueue)
                } else if ((step.childrenTotal ?? 0) > 0) {
                    tasks.push({ uri: node.uri, instance: node.instance, step })
                }
            }
        }
        enqueue(root)
        const worker = async (): Promise<void> => {
            for (let task = tasks.pop(); task; task = tasks.pop()) {
                const collected: CallNodeView[] = []
                const total = task.step.childrenTotal ?? 0
                let pageSize = -1
                // Page until the reported total is collected; an empty or shrinking page ends the loop
                // early rather than spinning on a total the server no longer agrees with.
                while (collected.length < total && pageSize !== 0) {
                    const page = await traceService.getTreeChildren(
                        projectId, task.uri, task.instance, task.step.ref, collected.length, SIMPLE_FETCH_PAGE)
                    if (get().runId !== token) return
                    pageSize = page.children?.length ?? 0
                    collected.push(...(page.children ?? []))
                }
                children[treeChildKey(task.uri, task.instance, task.step.ref)] = collected
                collected.forEach(enqueue)
                set({ simpleLoadedCount: loaded })
            }
        }
        try {
            // A few workers drain the queue in parallel; each downloaded level may enqueue deeper ones.
            await Promise.all(Array.from({ length: SIMPLE_FETCH_CONCURRENCY }, worker))
        } catch (error: any) {
            if (get().runId === token) {
                set({ error: error?.message || 'Failed to load the calculation tree' })
            }
            return null
        }
        return get().runId === token ? children : null
    }

    /** Restart the session from the top with the current settings, re-applying the user's breakpoints. */
    const restart = async (): Promise<void> => {
        const { projectId, breakpoints } = get()
        if (!projectId) return
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
    }

    /**
     * Restart the session for a business-mode inspect WITHOUT blanking the panel. The backend session is
     * cancelled and a fresh one started, but the last frame and its table stay on screen until the new stack
     * settles — so navigating within one table just moves the highlight instead of tearing the table down and
     * rebuilding it. Unlike {@link restart} it never routes through {@code terminate}, which clears the stack.
     */
    const restartQuietly = async (): Promise<void> => {
        const { projectId } = get()
        if (!projectId) return
        restarting = true
        try {
            try {
                await traceService.cancelTrace(projectId)
            } catch {
                // The fresh session started next supersedes it.
            }
            await get().start()
        } finally {
            restarting = false
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
            beginRun({ status: 'pending' })
            try {
                // Attach to a session already created by the launcher; otherwise start a new one.
                let stack: DebugStackView
                try {
                    stack = await traceService.getStack(projectId)
                } catch {
                    stack = await traceService.startTrace(projectId, launchOptions(
                        { tableId, fromModule, testRanges, inputJson },
                        { stopAtEntry: true, ...(get().profiling ? { profiling: true } : {}) }
                    ))
                }
                applyStack(stack)
            } catch (error: any) {
                set({ status: 'error', error: error?.message || 'Failed to start trace' })
            } finally {
                set({ loading: false })
            }
        },

        refreshStack: async () => {
            const { projectId, runId } = get()
            if (!projectId) return
            try {
                const stack = await traceService.getStack(projectId)
                // A newer run (rerun/restart bumps runId) or a project switch may have started while this
                // fetch was in flight; its result is stale, so dropping it keeps the newer run's frames and
                // tree instead of overwriting them with a finished run's settled stack.
                if (get().projectId !== projectId || get().runId !== runId) return
                applyStack(stack)
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
            const { projectId, status, stackVersion, advanced, simpleFocus } = get()
            if (!projectId) return
            set({ selectedFrameIndex: index })
            // A focused step in the business view shows only its own inputs, result and cell — all from the
            // step-inputs endpoint. The heavy frame-variables payload (every cell value, the grid, the
            // decision breakdown) is never rendered for a focused step, so don't fetch it.
            if (!advanced && simpleFocus) {
                set({ variables: null, variablesLoading: false })
                return
            }
            if (!isInspectable(status)) {
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
            if (!get().projectId || get().profiling === value) return
            // The executed tree can only be captured from the start, so switching restarts the session.
            set({ profiling: value })
            await restart()
        },

        /** Re-run the whole trace from the top with the current table, input, profiling, and watches. */
        rerun: async () => {
            await restart()
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
            beginRun()
            try {
                // Run the whole trace to completion (not the full tree) so every execution is captured.
                await get().terminate()
                const stack = await traceService.startTrace(projectId, launchOptions(
                    { tableId, fromModule, testRanges, inputJson },
                    { stopAtEntry: false, includeTree: false }
                ))
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

        onSocketStatus: (status, message, sessionId) => {
            // Sessions of the same user and table share one socket topic, so a stale session reaped in
            // the background reports its termination here too — such foreign events must not touch the
            // session this window is watching.
            const own = get().sessionId
            if (sessionId != null && own != null && sessionId !== own) {
                return
            }
            if (status === 'suspended') {
                // A synchronous step applies the authoritative stack from its own response; the WS
                // notification for that same suspension would only trigger a duplicate stack+variables fetch.
                if (get().loading) return
                void get().refreshStack()
            } else if (status === 'running') {
                // Same guard as 'suspended': a synchronous step owns the status via its own response, so
                // ignore the WS 'running' for that in-flight transition rather than clobbering the settled state.
                if (get().loading) return
                set({ status: 'running' })
            } else if (isTraceExecutionTerminal(status)) {
                // A quiet inspect restart cancels the previous session; ignore its 'terminated' echo so the
                // panel keeps the last table on screen between the cancel and the fresh stack.
                if (restarting) return
                // Show an immediate summary from the socket (if any); the full error is fetched below.
                set({
                    status,
                    frames: [],
                    selectedFrameIndex: null,
                    variables: null,
                    variablesLoading: false,
                    debugError: status === 'error' && message ? { summary: message } : null,
                    // Nothing is paused any more (e.g. an inspected row was conditionally skipped and the
                    // run finished), so the next click cannot resume from here.
                    simpleLastInspected: null,
                })
                if (status === 'error') {
                    void get().fetchTerminalError()
                } else if (status === 'completed') {
                    // A finished run still has a readable stack: the root call with its steps and result —
                    // plus, when profiling, the executed tree and the hot-spots profile. The socket only
                    // reports the status, so fetch the settled stack to show it. A terminated session is
                    // gone (for example when toggling profiling restarts it), so it is never fetched.
                    void get().refreshStack()
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

        setAdvanced: async (value) => {
            if (get().advanced === value) return
            set({ advanced: value })
            const { projectId, breakpoints } = get()
            if (!value && breakpoints.length > 0) {
                // The business view debugs with no breakpoints — drop any set while in the advanced mode.
                set({ breakpoints: [], breakpointLabels: {}, transientBreakpoint: null })
                if (projectId) {
                    try {
                        await traceService.setBreakpoints(projectId, [])
                    } catch {
                        // Best effort: the restart below starts a fresh session with none anyway.
                    }
                }
            }
            // Switching views restarts the trace from the top (like toggling profiling), so neither view
            // inherits the other's mid-run position; the business snapshot resets to its Run prompt.
            set({ ...SIMPLE_SNAPSHOT_RESET })
            if (projectId) {
                await restart()
            }
        },

        setShowDetailed: (value) => set({ showDetailed: value }),

        simpleRun: async () => {
            const { projectId, tableId, fromModule, testRanges, inputJson } = get()
            if (!projectId || !tableId || get().simpleLoading) return
            beginRun({ ...SIMPLE_SNAPSHOT_RESET, simpleLoading: true })
            const token = get().runId
            try {
                // The simple run debugs with no breakpoints, so it always reaches the end in one go.
                if (get().breakpoints.length > 0) {
                    set({ breakpoints: [], breakpointLabels: {}, transientBreakpoint: null })
                    try {
                        await traceService.setBreakpoints(projectId, [])
                    } catch {
                        // Best effort: the fresh session started below has no breakpoints anyway.
                    }
                }
                await get().terminate()
                set({ status: 'running' })
                // One full profiled run: the response arrives once the whole calculation has finished,
                // carrying the executed tree to download.
                const stack = await traceService.startTrace(projectId, launchOptions(
                    // Detailed titles (signature, result, cell values) are the business view's default — the
                    // advanced debugger never asks for them, keeping its tree lean.
                    { tableId, fromModule, testRanges, inputJson },
                    { stopAtEntry: false, profiling: true, detailedTitles: true }
                ))
                if (get().runId !== token) return
                applyStack(stack)
                const tree = stack.tree ?? null
                set({ simpleTree: tree, simpleTotalCount: stack.profile?.nodeCount ?? null })
                if (tree) {
                    const children = await downloadTree(projectId, tree, token)
                    if (children) {
                        set({
                            simpleChildren: children,
                            simpleOrder: buildSimpleOrder(tree, children),
                            simpleReady: true,
                        })
                    }
                }
            } catch (error: any) {
                if (get().runId === token) {
                    set({ status: 'error', error: error?.message || 'Failed to run the trace' })
                }
            } finally {
                if (get().runId === token) {
                    set({ loading: false, simpleLoading: false })
                }
            }
        },

        simpleInspect: async ({ key, frameUri, frameInstance, stepType, label, focus, selectionKey }) => {
            const { projectId, simpleReady, simpleLoading, loading, status } = get()
            if (!projectId || !simpleReady || simpleLoading || loading || status === 'running') return
            // Clicking the row already on screen is a no-op — its inputs and result are shown, nothing to re-run.
            const clickedKey = selectionKey ?? key
            if (get().simpleSelectedKey === clickedKey) return
            const order = get().simpleOrder[key] ?? null
            const last = get().simpleLastInspected
            // The highlight follows the clicked row (selectionKey); the run follows the target (key).
            set({ simpleSelectedKey: clickedKey, simpleLastInspected: order, simpleFocus: focus ?? null })
            // Execution can only move forward: a row is still reachable by resuming when it starts after
            // everything the previous inspection already ran — its own subtree included. Anything else
            // (an earlier row, or a sub-call of the inspected one) has executed and needs a fresh run.
            const canResume = status === 'suspended' && last !== null && order !== null && order.pre > last.end
            if (!canResume) {
                await restartQuietly()
                if (get().status !== 'suspended') return
            }
            // Execute in place only when paused exactly at the target: a call at its fresh entry (the
            // root right after a restart), or the owning frame already ON the clicked step's line (the
            // very next step after the previously inspected one). There the target's own entry point is
            // already behind us, so an inclusive breakpoint would never fire — one step finishes it.
            const top = get().frames.at(-1)
            const atTarget = top && top.uri === frameUri && top.instance === frameInstance && !top.completed
                && (stepType === 'over' ? top.location?.ref === focus?.ref : !top.location)
            if (atTarget) {
                await (stepType === 'over' ? get().stepOver() : get().stepOut())
            } else {
                // An inclusive one-shot breakpoint: the engine runs the target and suspends right after
                // it executed, its inputs and result on the stack — a single stop, no follow-up steps.
                await get().runTo(`after:${key}`, label)
            }
        },

        fetchTreeChildren: async (uri, instance, step) => {
            const key = treeChildKey(uri, instance, step)
            const { projectId, treeChildren, treeLoading, stackVersion } = get()
            // Ignore while a page for this step is already in flight, so a double-click can't page twice.
            if (!projectId || treeLoading[key]) {
                return
            }
            const offset = treeChildren[key]?.length ?? 0
            set(s => ({ treeLoading: { ...s.treeLoading, [key]: true } }))
            try {
                const page = await traceService.getTreeChildren(projectId, uri, instance, step, offset, TREE_PAGE_SIZE)
                // Drop a page that arrives after a re-run: instance indices restart at 0, so the same (uri,
                // instance) key would otherwise be reused by the new run's node at that position, pinning the
                // previous run's sub-calls with no refetch.
                if (get().stackVersion !== stackVersion) {
                    return
                }
                set(s => ({
                    treeChildren: { ...s.treeChildren, [key]: [...(s.treeChildren[key] ?? []), ...(page.children ?? [])]},
                    treeLoading: { ...s.treeLoading, [key]: false },
                }))
            } catch (error: any) {
                set(s => ({ treeLoading: { ...s.treeLoading, [key]: false } }))
                notification.error({ title: error?.message || 'Failed to load sub-calls' })
            }
        },

        reset: () => set(initialState),
    }
})
