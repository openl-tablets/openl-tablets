import { notification } from 'antd'
import CONFIG from './config'
import { useAppStore } from 'store'

const fetchInitialConfig = {
    method: 'GET',
    headers: new Headers(),
}

const appStore = useAppStore.getState()

const apiCall = async (url: string, params?: RequestInit, throwError = false) => {
    const responseParams = {
        ...fetchInitialConfig,
        ...params,
    }

    return fetch(`${CONFIG.CONTEXT}/web${url}`, responseParams)
        .then(async response => {
            const { status } = response
            if (status === 200) {
                const { headers } = response
                const contentType = headers.get('Content-Type')
                if (contentType && contentType.indexOf('application/json') !== -1) {
                    return response.json()
                }
                return response.text()
            } else if (status > 200 && status < 300) {
                return true
            }
            else if (status === 401) {
                appStore.setShowLogin(true)
                throw new Error('Unauthorized! Please log in.')
            } else if (status === 403) {
                appStore.setShowForbidden(true)
                throw new Error('Forbidden! You do not have permission to access this resource.')
            } else if (status === 404) {
                appStore.setShowNotFound(true)
                throw new Error('Page not found!')
            } else if (status === 500) {
                appStore.setShowServerError(true)
                throw new Error('Internal server error! Please try again later.')
            } else {
                await response.json().then((data: any) => {
                    if (data.fields) {
                        const errors = data.fields.map(({ message }: { message: any }) => message)
                        throw new Error(errors)
                    } else {
                        throw new Error(data.message)
                    }
                })
                throw new Error('Something went wrong on API server!')
            }
        })
        .catch(error => {
            if (throwError) {
                throw error
            } else {
                notification.error({ message: error.toString() })
            }
        })
        .finally((result: any = false) => {
            return result
        })
}

export default apiCall
