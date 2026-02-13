import apiCall from './apiCall'
import type { ApiCallOptions } from './apiCall'
import CONFIG from './config'
import { errorHandler } from 'utils/errorHandling'
import type {
    TraceNodeView,
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
    if (error instanceof TypeError) {
        // Network errors like "Failed to fetch"
        return true
    }

    if (error instanceof Error) {
        const message = error.message.toLowerCase()
        // Check for transient HTTP status codes in error messages
        // 502 Bad Gateway, 503 Service Unavailable, 504 Gateway Timeout
        if (message.includes('502') || message.includes('503') || message.includes('504')) {
            return true
        }
        // Network-related error messages
        if (message.includes('network') || message.includes('timeout') || message.includes('econnreset')) {
            return true
        }
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

/**
 * API service for trace execution endpoints.
 * All endpoints require project-level READ access.
 * All methods use retry logic for transient network errors.
 */
export const traceService = {
    /**
     * Get node children for lazy loading, or root nodes if nodeId is not provided.
     * @param nodeId Parent node ID to get children for (omit for root nodes)
     */
    getNodeChildren: async (
        projectId: string,
        nodeId?: number,
        showRealNumbers = false
    ): Promise<TraceNodeView[]> => {
        const params = new URLSearchParams()
        if (nodeId !== undefined) {
            params.set('id', String(nodeId))
        }
        params.set('showRealNumbers', String(showRealNumbers))

        return await retryApiCall<TraceNodeView[]>(
            `/projects/${encodeURIComponent(projectId)}/trace/nodes?${params.toString()}`,
            undefined,
            TRACE_API_OPTIONS
        )
    },

    /**
     * Get full node details including parameters, context, result, errors.
     * @param nodeId Node ID to get details for
     */
    getNodeDetails: async (
        projectId: string,
        nodeId: number,
        showRealNumbers = false
    ): Promise<TraceNodeView> => {
        return await retryApiCall<TraceNodeView>(
            `/projects/${encodeURIComponent(projectId)}/trace/nodes/${nodeId}?showRealNumbers=${showRealNumbers}`,
            undefined,
            TRACE_API_OPTIONS
        )
    },

    /**
     * Get lazy-loaded parameter value.
     * @param parameterId Parameter ID from TraceParameterValue
     */
    getParameterValue: async (
        projectId: string,
        parameterId: number
    ): Promise<TraceParameterValue> => {
        return await retryApiCall<TraceParameterValue>(
            `/projects/${encodeURIComponent(projectId)}/trace/parameters/${parameterId}`,
            undefined,
            TRACE_API_OPTIONS
        )
    },

    /**
     * Get traced table HTML fragment with highlighted cells.
     * @param nodeId Node ID to get table for
     * @param showFormulas Show formulas instead of values
     */
    getTraceTableHtml: async (
        projectId: string,
        nodeId: number,
        showFormulas = false
    ): Promise<string> => {
        return await retryApiCall<string>(
            `/projects/${encodeURIComponent(projectId)}/trace/nodes/${nodeId}/table?showFormulas=${showFormulas}`,
            { headers: { Accept: 'text/html' } },
            TRACE_API_OPTIONS
        )
    },

    /**
     * Cancel ongoing trace execution.
     * Returns 204 on success, 404 if no trace exists.
     */
    cancelTrace: async (projectId: string): Promise<void> => {
        return await retryApiCall<void>(
            `/projects/${encodeURIComponent(projectId)}/trace`,
            { method: 'DELETE' },
            TRACE_API_OPTIONS
        )
    },

    /**
     * Start trace execution.
     * @param projectId Base64 encoded "repositoryId:projectName"
     * @param options tableId, testRanges, fromModule, inputJson
     * @returns Promise that resolves on 202 Accepted
     */
    startTrace: async (
        projectId: string,
        options: {
            tableId: string
            testRanges?: string
            fromModule?: string
            inputJson?: string
        }
    ): Promise<void> => {
        const params = new URLSearchParams()
        params.set('tableId', options.tableId)
        if (options.testRanges) params.set('testRanges', options.testRanges)
        if (options.fromModule) params.set('fromModule', options.fromModule)

        return await retryApiCall<void>(
            `/projects/${encodeURIComponent(projectId)}/trace?${params.toString()}`,
            {
                method: 'POST',
                headers: options.inputJson ? { 'Content-Type': 'application/json' } : {},
                body: options.inputJson || undefined
            },
            TRACE_API_OPTIONS
        )
    },

    /**
     * Export trace as text file download.
     * @param projectId Project ID
     * @param showRealNumbers Show real numbers instead of formatted values
     * @param release Whether to clear trace from memory after download
     */
    exportTrace: (
        projectId: string,
        showRealNumbers: boolean,
        release: boolean = false
    ): void => {
        const url = `${CONFIG.CONTEXT}/web/projects/${encodeURIComponent(projectId)}/trace/export?showRealNumbers=${showRealNumbers}&release=${release}`
        window.location.href = url
    },
}

export default traceService
