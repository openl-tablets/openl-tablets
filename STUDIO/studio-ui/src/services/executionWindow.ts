import CONFIG from './config'
import { readJson, writeJson } from 'utils/localStore'
import type { RunFileOptions, TestsQuery } from './execution'
import { RESIZABLE_WINDOW_FEATURES } from './windowFeatures'

/** The last result screen opened by this browser, including the options chosen when the run started. */
export type ExecutionWindowRequest =
    | { kind: 'tests', projectId: string, tableId?: string | undefined, options?: Partial<TestsQuery> }
    | { kind: 'run', projectId: string, tableId: string, fileOptions: RunFileOptions }
    | { kind: 'benchmark', projectId: string, tableId: string }

export const EXECUTION_WINDOW_KEY = 'openl.execution.window'
export const EXECUTION_EDITOR_NAVIGATION = 'openl:execution:navigate'
let launchSequence = 0

/** A saved window may outlive the result retained by the server. */
export const isMissingExecution = (error: unknown): boolean =>
    error !== null && typeof error === 'object' && 'status' in error && error.status === 404

/** Publishes a new execution to a result window that may already be open. Returns false if storage refused it. */
export const publishExecution = (request: ExecutionWindowRequest): boolean => {
    // A second run can have identical options. The fresh id still delivers a storage event to the open window.
    const saved = {
        request,
        id: `${performance.timeOrigin}-${Date.now()}-${++launchSequence}`,
    }
    writeJson(EXECUTION_WINDOW_KEY, saved)
    return readJson<SavedExecution | null>(EXECUTION_WINDOW_KEY, null, isSavedExecution)?.id === saved.id
}

const isExecutionRequest = (value: unknown): value is ExecutionWindowRequest => {
    if (!value || typeof value !== 'object' || !('kind' in value) || !('projectId' in value)
        || typeof value.projectId !== 'string') {
        return false
    }
    if (value.kind === 'tests') return true
    if (!('tableId' in value) || typeof value.tableId !== 'string') return false
    return value.kind === 'benchmark' || (value.kind === 'run' && 'fileOptions' in value
        && value.fileOptions !== null && typeof value.fileOptions === 'object')
}

interface SavedExecution {
    request: ExecutionWindowRequest
    id: string
}

const isSavedExecution = (value: unknown): value is SavedExecution =>
    value !== null && typeof value === 'object' && 'request' in value && isExecutionRequest(value.request)
    && 'id' in value && typeof value.id === 'string'

/** The result window reads this again after a refresh. */
export const currentExecution = (): ExecutionWindowRequest | null =>
    readJson<SavedExecution | null>(EXECUTION_WINDOW_KEY, null, isSavedExecution)?.request ?? null

/** Opens or focuses the one result window used by Test, Run and Benchmark. */
export const openExecutionWindow = (request: ExecutionWindowRequest): boolean => {
    if (!publishExecution(request)) return false
    const url = `${CONFIG.CONTEXT}/execution-results`
    return window.open(url, 'execution_results', RESIZABLE_WINDOW_FEATURES) !== null
}
