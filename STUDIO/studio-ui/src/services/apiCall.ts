import { notification } from 'antd'
import CONFIG from './config'
import { useAppStore } from 'store'

const fetchInitialConfig = {
    method: 'GET',
    headers: new Headers(),
}

class EmptyError extends Error {
    constructor() {
        super('')
        this.name = 'EmptyError'
    }
}

class ApiHttpError extends Error {
    status: number
    payload?: unknown

    constructor(status: number, message: string, payload?: unknown) {
        super(message)
        this.name = 'ApiHttpError'
        this.status = status
        this.payload = payload
    }
}

class NotFoundError extends ApiHttpError {
    constructor(message?: string, payload?: unknown) {
        super(404, message || 'Not found', payload)
        this.name = 'NotFoundError'
    }
}

class ForbiddenError extends ApiHttpError {
    constructor(message = 'Forbidden', payload?: unknown) {
        super(403, message, payload)
        this.name = 'ForbiddenError'
    }
}

const appStore = useAppStore.getState()

/**
 * Extract error message from response body.
 * Tries to parse JSON and extract 'message' field, falls back to default message.
 */
const extractErrorMessage = async (response: Response, defaultMessage: string): Promise<string> => {
    try {
        const contentType = response.headers.get('Content-Type')
        if (contentType && contentType.indexOf('application/json') !== -1) {
            const data = await response.json()
            return data.message || defaultMessage
        }
    } catch {
        // Ignore parse errors, use default message
    }
    return defaultMessage
}

export interface ApiCallOptions {
    throwError?: boolean
    suppressErrorPages?: boolean // If true, don't show error pages (404, 403, 500) - useful when 404 is expected
}

const isJsonResponse = (response: Response): boolean => {
    const contentType = response.headers.get('Content-Type')
    return Boolean(contentType && contentType.indexOf('application/json') !== -1)
}

const tryParseJsonBody = async (response: Response): Promise<unknown> => {
    if (!isJsonResponse(response)) {
        return undefined
    }
    try {
        return await response.json()
    } catch {
        return undefined
    }
}

const getErrorMessage = (payload: unknown, fallbackMessage: string): string => {
    if (payload && typeof payload === 'object' && 'message' in payload) {
        const message = (payload as { message?: unknown }).message
        if (typeof message === 'string' && message.trim() !== '') {
            return message
        }
    }
    return fallbackMessage
}

const isApiHttpError = (error: unknown): error is ApiHttpError =>
    error instanceof ApiHttpError

const apiCall = async (
    url: string,
    params?: RequestInit,
    options: boolean | ApiCallOptions = false
) => {
    // Support legacy boolean signature for backward compatibility
    const opts: ApiCallOptions = typeof options === 'boolean'
        ? { throwError: options }
        : options || {}

    const responseParams = {
        ...fetchInitialConfig,
        ...params,
    }

    return fetch(`${CONFIG.CONTEXT}/web${url}`, responseParams)
        .then(async response => {
            const { status } = response
            if (status >= 200 && status < 300) {
                const { headers } = response
                const contentType = headers.get('Content-Type')
                if (contentType && contentType.indexOf('application/json') !== -1) {
                    return response.json()
                }
                // For 204 No Content or responses without body
                if (status === 204) {
                    return true
                }
                const text = await response.text()
                return text || true
            }
            else if (status === 401) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowLogin(true)
                }
                throw new EmptyError()
            } else if (status === 403) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowForbidden(true)
                }
                // Try to extract error message from response body
                const errorMessage = await extractErrorMessage(response, 'Forbidden! You do not have permission to access this resource.')
                throw new ForbiddenError(errorMessage)
            } else if (status === 404) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowNotFound(true)
                }
                // Try to extract error message from response body
                const errorMessage = await extractErrorMessage(response, 'Not found')
                throw new NotFoundError(errorMessage)
            } else if (status === 500) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowServerError(true)
                }
                const payload = await tryParseJsonBody(response)
                throw new ApiHttpError(
                    status,
                    getErrorMessage(payload, 'Internal server error! Please try again later.'),
                    payload
                )
            } else {
                const payload = await tryParseJsonBody(response)
                if (payload && typeof payload === 'object' && 'fields' in payload && Array.isArray((payload as { fields: unknown[] }).fields)) {
                    const errors = (payload as { fields: Array<{ message: unknown }> }).fields
                        .map(({ message }) => (typeof message === 'string' ? message.trim() : String(message ?? '').trim()))
                        .filter(Boolean)
                    const errorMessage = errors.length > 0
                        ? errors.join('\n')
                        : getErrorMessage(payload, 'Something went wrong on API server!')
                    throw new Error(errorMessage)
                } else {
                    throw new ApiHttpError(
                        status,
                        getErrorMessage(payload, 'Something went wrong on API server!'),
                        payload
                    )
                }
            }
        })
        .catch(error => {
            if (opts.throwError) {
                throw error
            } else if (error instanceof EmptyError) {
            } else if (error instanceof Error) {
                notification.error({ title: error.toString() })
            }
        })
        .finally((result: any = false) => {
            return result
        })
}

export { ApiHttpError, NotFoundError, EmptyError, ForbiddenError, isApiHttpError }
export default apiCall
