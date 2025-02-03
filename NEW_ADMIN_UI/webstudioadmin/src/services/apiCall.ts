import { notification } from 'antd'

const baseURL = process.env.API_URL || ''

const fetchInitialConfig = {
    method: 'GET',
    headers: new Headers(),
}

const apiCall = async (url: string, params?: RequestInit, throwError = false) => {
    const responseParams = {
        ...fetchInitialConfig,
        ...params,
    }

    return fetch(`${baseURL}${url}`, responseParams)
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
            } else if (status === 404) {
                throw new Error('Page not found!')
            } else {
                await response.json().then((data: any) => {
                    if (data.fields) {
                        const errors = data.fields.map(({ message }) => message)
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
