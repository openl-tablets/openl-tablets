import { create } from 'zustand'
import { notification } from 'antd'
import type {
    DebugFrameVariables,
    DebugFrameView,
    DebugStackView,
    DebugStatus,
    TraceParameterValue,
} from 'types/trace'
import traceService from 'services/traceService'

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
    errorMessage: string | null
    selectedFrameIndex: number | null
    variables: DebugFrameVariables | null
    variablesLoading: boolean
    /** Increments on every suspension so views that depend on the current line (table highlight) refresh. */
    stackVersion: number
    breakpoints: string[]
    breakpointLabels: Record<string, string>

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
    stepInto: () => Promise<void>
    stepOver: () => Promise<void>
    stepOut: () => Promise<void>
    resume: () => Promise<void>
    pause: () => Promise<void>
    terminate: () => Promise<void>
    loadBreakpoints: () => Promise<void>
    toggleBreakpoint: (uri: string, label?: string) => Promise<void>
    onSocketStatus: (status: DebugStatus, message?: string) => void
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
    errorMessage: null,
    selectedFrameIndex: null,
    variables: null,
    variablesLoading: false,
    stackVersion: 0,
    breakpoints: [],
    breakpointLabels: {},
    loading: false,
    error: null,
}

const isSuspended = (status: DebugStatus | null): boolean => status === 'SUSPENDED'

export const useTraceStore = create<DebugState>((set, get) => {
    /** Apply a freshly fetched stack, auto-selecting the current (top) frame when suspended. */
    const applyStack = (stack: DebugStackView): void => {
        const topIndex = stack.frames.length > 0 ? stack.frames.length - 1 : null
        set({
            status: stack.status,
            frames: stack.frames,
            errorMessage: stack.errorMessage ?? null,
            selectedFrameIndex: isSuspended(stack.status) ? topIndex : null,
            variables: null,
            stackVersion: get().stackVersion + 1,
        })
        if (isSuspended(stack.status) && topIndex !== null) {
            void get().selectFrame(topIndex)
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
            set({ loading: true, error: null, status: 'PENDING' })
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
                    })
                }
                applyStack(stack)
            } catch (error: any) {
                set({ status: 'ERROR', error: error?.message || 'Failed to start trace' })
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

        selectFrame: async (index) => {
            const { projectId, status } = get()
            if (!projectId) return
            set({ selectedFrameIndex: index })
            if (!isSuspended(status)) {
                set({ variables: null })
                return
            }
            set({ variablesLoading: true })
            try {
                const variables = await traceService.getVariables(projectId, index)
                set({ variables, variablesLoading: false })
            } catch (error: any) {
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
            set({ status: 'RUNNING', variables: null })
            try {
                await traceService.resume(projectId)
            } catch (error: any) {
                set({ status: 'SUSPENDED', error: error?.message || 'Resume failed' })
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

        terminate: async () => {
            const { projectId } = get()
            if (!projectId) return
            try {
                await traceService.cancelTrace(projectId)
                set({ status: 'TERMINATED', frames: [], selectedFrameIndex: null, variables: null })
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
            if (status === 'SUSPENDED') {
                void get().refreshStack()
            } else if (status === 'RUNNING') {
                set({ status: 'RUNNING' })
            } else if (status === 'COMPLETED' || status === 'ERROR' || status === 'TERMINATED') {
                set({ status, errorMessage: message ?? get().errorMessage, frames: [], selectedFrameIndex: null, variables: null })
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
