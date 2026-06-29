import apiCall from './apiCall'
import { ApiHttpError } from './apiCall'
import type { ApiCallOptions } from './apiCall'
import { errorHandler } from 'utils/errorHandling'
import type {
    BreakpointTableView,
    CellHighlight,
    DebugFrameVariables,
    DebugStackView,
    RawTableView,
    StepType,
    TraceParameterValue,
} from 'types/trace'

/**
 * Configuration for retry behavior.
 */
interface RetryConfig {
    /** Maximum number of retry attempts (default: 3) */
    maxAttempts?: number
    /** Initial delay in milliseconds before first retry (default: 500) */
    initialDelayMs?: number
    /** Maximum delay in milliseconds between retries (default: 5000) */
    maxDelayMs?: number
    /** Multiplier for exponential backoff (default: 2) */
    backoffMultiplier?: number
}

const DEFAULT_RETRY_CONFIG: Required<RetryConfig> = {
    maxAttempts: 3,
    initialDelayMs: 500,
    maxDelayMs: 5000,
    backoffMultiplier: 2,
}

/**
 * Determines if an error is transient and should be retried.
 * Transient errors include network failures and specific HTTP status codes.
 */
const isTransientError = (error: unknown): boolean => {
    if (error instanceof ApiHttpError) {
        return error.status === 502 || error.status === 503 || error.status === 504
    }

    if (error instanceof TypeError) {
        // Network errors like "Failed to fetch"
        return true
    }

    return false
}

/**
 * Delays execution for specified milliseconds.
 */
const delay = (ms: number): Promise<void> =>
    new Promise(resolve => setTimeout(resolve, ms))

/**
 * Executes an API call with configurable retry logic for transient errors.
 * Uses exponential backoff between retries.
 *
 * @param url API endpoint URL
 * @param params Optional fetch parameters
 * @param options API call options (throwError, suppressErrorPages)
 * @param retryConfig Optional retry configuration
 * @returns Promise resolving to the API response
 * @throws Re-throws the last error if all retries are exhausted
 */
const retryApiCall = async <T>(
    url: string,
    params?: RequestInit,
    options?: ApiCallOptions,
    retryConfig?: RetryConfig
): Promise<T> => {
    const config = { ...DEFAULT_RETRY_CONFIG, ...retryConfig }
    let lastError: unknown
    let currentDelay = config.initialDelayMs

    for (let attempt = 1; attempt <= config.maxAttempts; attempt++) {
        try {
            return await apiCall(url, params, options)
        } catch (error) {
            lastError = error

            // Log the failure
            if (error instanceof Error) {
                errorHandler.logError(error, {
                    message: `API call failed (attempt ${attempt}/${config.maxAttempts}): ${url}`,
                })
            }

            // Only retry on transient errors, and only if we have attempts left
            if (!isTransientError(error) || attempt >= config.maxAttempts) {
                break
            }

            // Wait before retrying with exponential backoff
            await delay(currentDelay)
            currentDelay = Math.min(currentDelay * config.backoffMultiplier, config.maxDelayMs)
        }
    }

    // All retries exhausted, rethrow the last error
    throw lastError
}

/** Standard API call options for trace endpoints */
const TRACE_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

const base = (projectId: string): string => `/projects/${encodeURIComponent(projectId)}/trace`

/**
 * API client for the interactive trace debugger.
 * All endpoints require project-level READ access and retry on transient network errors.
 */
export const traceService = {
    /**
     * Start a debug session. Returns the initial execution stack (suspended at entry or after
     * running to the first breakpoint).
     */
    startTrace: async (
        projectId: string,
        options: {
            tableId: string
            testRanges?: string
            fromModule?: string
            stopAtEntry?: boolean
            inputJson?: string
        }
    ): Promise<DebugStackView> => {
        const params = new URLSearchParams()
        params.set('tableId', options.tableId)
        if (options.testRanges) params.set('testRanges', options.testRanges)
        if (options.fromModule) params.set('fromModule', options.fromModule)
        params.set('stopAtEntry', String(options.stopAtEntry ?? true))

        return await retryApiCall<DebugStackView>(
            `${base(projectId)}?${params.toString()}`,
            {
                method: 'POST',
                headers: options.inputJson ? { 'Content-Type': 'application/json' } : {},
                ...(options.inputJson && { body: options.inputJson }),
            },
            TRACE_API_OPTIONS
        )
    },

    /** Get the current execution stack. */
    getStack: async (projectId: string): Promise<DebugStackView> =>
        retryApiCall<DebugStackView>(`${base(projectId)}/stack`, undefined, TRACE_API_OPTIONS),

    /** Step the suspended session and return the new stack. */
    step: async (projectId: string, type: StepType): Promise<DebugStackView> =>
        retryApiCall<DebugStackView>(
            `${base(projectId)}/step?type=${type}`,
            { method: 'POST' },
            TRACE_API_OPTIONS
        ),

    /** Resume to the next breakpoint (asynchronous; outcome arrives via WebSocket). */
    resume: async (projectId: string): Promise<void> =>
        retryApiCall<void>(`${base(projectId)}/resume`, { method: 'POST' }, TRACE_API_OPTIONS),

    /** Request an asynchronous suspend at the next safepoint. */
    pause: async (projectId: string): Promise<void> =>
        retryApiCall<void>(`${base(projectId)}/pause`, { method: 'POST' }, TRACE_API_OPTIONS),

    /** Freeze and return a stack frame's variables (must be suspended). */
    getVariables: async (projectId: string, frameIndex: number): Promise<DebugFrameVariables> =>
        retryApiCall<DebugFrameVariables>(
            `${base(projectId)}/frames/${frameIndex}/variables`,
            undefined,
            TRACE_API_OPTIONS
        ),

    /**
     * Get the raw grid of a table (Tables API). The structure is immutable during a session, so the
     * client fetches it once per table and overlays per-step highlights on top.
     */
    getRawTable: async (
        projectId: string,
        tableId: string,
        maxRows?: number,
        styles?: boolean
    ): Promise<RawTableView> => {
        const cap = maxRows != null ? `&maxRows=${maxRows}` : ''
        const withStyles = styles ? '&styles=true' : ''
        return retryApiCall<RawTableView>(
            `/projects/${encodeURIComponent(projectId)}/tables/${encodeURIComponent(tableId)}?raw=true${cap}${withStyles}`,
            undefined,
            TRACE_API_OPTIONS
        )
    },

    /** Get the cells to highlight on a stack frame's table, keyed by A1 address. */
    getFrameHighlights: async (projectId: string, frameIndex: number): Promise<CellHighlight[]> =>
        retryApiCall<CellHighlight[]>(
            `${base(projectId)}/frames/${frameIndex}/highlights`,
            undefined,
            TRACE_API_OPTIONS
        ),

    /** Get a lazy-loaded parameter value. */
    getParameterValue: async (projectId: string, parameterId: number): Promise<TraceParameterValue> =>
        retryApiCall<TraceParameterValue>(
            `${base(projectId)}/parameters/${parameterId}`,
            undefined,
            TRACE_API_OPTIONS
        ),

    /** Get the breakpoints (table URIs). */
    getBreakpoints: async (projectId: string): Promise<string[]> =>
        retryApiCall<string[]>(`${base(projectId)}/breakpoints`, undefined, TRACE_API_OPTIONS),

    /** List rule tables that can be a breakpoint target, to set a breakpoint by name before it runs. */
    getBreakpointTables: async (projectId: string): Promise<BreakpointTableView[]> =>
        retryApiCall<BreakpointTableView[]>(
            // Only the name is needed to set and label a breakpoint; trim the rest with field projection.
            `${base(projectId)}/breakpoint-tables?fields=name`,
            undefined,
            TRACE_API_OPTIONS
        ),

    /** Replace the breakpoint set. */
    setBreakpoints: async (projectId: string, uris: string[]): Promise<void> =>
        retryApiCall<void>(
            `${base(projectId)}/breakpoints`,
            {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ uris }),
            },
            TRACE_API_OPTIONS
        ),

    /** Terminate the debug session. Returns 204 on success, 404 if none. */
    cancelTrace: async (projectId: string): Promise<void> =>
        retryApiCall<void>(base(projectId), { method: 'DELETE' }, TRACE_API_OPTIONS),
}

export default traceService
