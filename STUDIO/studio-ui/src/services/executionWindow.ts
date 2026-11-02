import CONFIG from './config'
import { errorMessage } from 'utils/errorMessage'
import { readJson, writeJson } from 'utils/localStore'
import type { ExecutionKind, RunFileOptions, TestsQuery } from './execution'
import { RESIZABLE_WINDOW_FEATURES } from './windowFeatures'

/** An execution the window shows, including the options chosen when it started. */
type StartedExecution =
    | { kind: 'tests', projectId: string, tableId?: string | undefined, options?: Partial<TestsQuery> }
    | { kind: 'run', projectId: string, tableId: string, fileOptions: RunFileOptions }
    | { kind: 'benchmark', projectId: string, tableId: string }

/**
 * The last result screen opened by this browser: an execution that started, one that is starting, or one that
 * could not start and why.
 */
export type ExecutionWindowRequest =
    | StartedExecution
    | { kind: 'starting', of: ExecutionKind }
    | { kind: 'failed', of: ExecutionKind, message: string }

export const EXECUTION_WINDOW_KEY = 'openl.execution.window'
export const EXECUTION_EDITOR_NAVIGATION = 'openl:execution:navigate'
let launchSequence = 0

const hasStatus = (error: unknown, status: number): boolean =>
    error !== null && typeof error === 'object' && 'status' in error && error.status === status

/** A saved window may outlive the result retained by the server. */
export const isMissingExecution = (error: unknown): boolean => hasStatus(error, 404)

/** A project was changed or reloaded since the execution started, so the server dropped what it produced. */
export const isOutdatedExecution = (error: unknown): boolean => hasStatus(error, 410)

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

/** Every kind of execution: a kind added to {@link ExecutionKind} fails to compile until it is listed here. */
const EXECUTION_KINDS: Record<ExecutionKind, true> = { tests: true, run: true, benchmark: true }

const isExecutionKind = (value: unknown): value is ExecutionKind =>
    typeof value === 'string' && Object.hasOwn(EXECUTION_KINDS, value)

/** A launch that has not started an execution yet, or could not start one. */
const isLaunch = (value: object): boolean => {
    if (!('kind' in value) || !('of' in value) || !isExecutionKind(value.of)) return false
    return value.kind === 'starting' || (value.kind === 'failed' && 'message' in value && typeof value.message === 'string')
}

const isExecutionRequest = (value: unknown): value is ExecutionWindowRequest => {
    if (!value || typeof value !== 'object') {
        return false
    }
    if (isLaunch(value)) {
        return true
    }
    if (!('kind' in value) || !('projectId' in value) || typeof value.projectId !== 'string') {
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

/** Whether the window shows the page at the path. A window showing a page of another site shows none of ours. */
const showsPage = (target: Window, path: string): boolean => {
    try {
        return target.location.pathname === path
    } catch {
        return false
    }
}

/**
 * Brings up the one result window used by Test, Run and Benchmark.
 *
 * A window that shows the results already is focused as it is: it follows the next execution through the storage
 * event, without loading the page again. Any other window is sent to the results page.
 *
 * @return false when the browser refuses the window
 */
const openResultsWindow = (): boolean => {
    const path = `${CONFIG.CONTEXT}/execution-results`
    const results = window.open('', 'execution_results', RESIZABLE_WINDOW_FEATURES)
    if (results === null) return false
    if (!showsPage(results, path)) {
        results.location.href = path
    }
    results.focus()
    return true
}

/**
 * Shows the request in the result window, opening or focusing it.
 *
 * Nothing is published when the browser refuses the window, so a window opened later shows what it showed before.
 * The page of a window just opened loads after the request is published, and reads it.
 */
export const openExecutionWindow = (request: ExecutionWindowRequest): boolean =>
    openResultsWindow() && publishExecution(request)

/** What a launch says when the browser refuses what the results window needs. */
export interface WindowRefusals {
    /** The browser would not open the window. */
    blocked: string
    /** The browser would not store the launch the window reads. */
    unsaved: string
}

/**
 * Starts an execution and shows it in the results window.
 *
 * The window opens first, while the browser still counts the click that asked for it: starting the execution
 * waits for a project that may still be compiling, and a window asked for after that may be refused. The window
 * says the execution is starting until the start answers, then follows it, or says why it could not start.
 *
 * Rejects without starting anything when the browser will not open the window, or will not store the launch the
 * window reads; the window shows nothing of it. Rejects with the reason, which the window shows, when the execution
 * cannot start.
 *
 * @param of      what is started
 * @param start   starts the execution and answers what the window shows of it
 * @param refused what the rejection says when the browser refuses the window or the storage: how to allow it
 */
export const launchInWindow = async (
    of: ExecutionKind,
    start: () => Promise<ExecutionWindowRequest>,
    refused: WindowRefusals
): Promise<void> => {
    if (!openResultsWindow()) {
        throw new Error(refused.blocked)
    }
    if (!publishExecution({ kind: 'starting', of })) {
        throw new Error(refused.unsaved)
    }
    let started: ExecutionWindowRequest
    try {
        started = await start()
    } catch (error) {
        publishExecution({ kind: 'failed', of, message: errorMessage(error) })
        throw error
    }
    publishExecution(started)
}
