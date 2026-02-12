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

class NotFoundError extends Error {
    constructor() {
        super('Not found')
        this.name = 'NotFoundError'
    }
}

class ForbiddenError extends Error {
    constructor(message = 'Forbidden') {
        super(message)
        this.name = 'ForbiddenError'
    }
}

const appStore = useAppStore.getState()

interface ApiCallOptions {
    throwError?: boolean
    suppressErrorPages?: boolean // If true, don't show error pages (404, 403, 500) - useful when 404 is expected
}

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
                throw new ForbiddenError('Forbidden! You do not have permission to access this resource.')
            } else if (status === 404) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowNotFound(true)
                }
                throw new NotFoundError()
            } else if (status === 500) {
                if (!opts.suppressErrorPages) {
                    appStore.setShowServerError(true)
                }
                throw new Error('Internal server error! Please try again later.')
            } else {
                const data = await response.json()
                if (data.fields) {
                    const errors = data.fields.map(({ message }: { message: any }) => message)
                    throw new Error(errors)
                } else {
                    throw new Error(data.message || 'Something went wrong on API server!')
                }
            }
        })
        .catch(error => {
            if (opts.throwError) {
                throw error
            } else if (error instanceof EmptyError) {
            } else if (error instanceof Error) {
                notification.error({ message: error.toString() })
            }
        })
        .finally((result: any = false) => {
            return result
        })
}

export { NotFoundError, EmptyError, ForbiddenError }
export default apiCall
