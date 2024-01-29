import { notification } from 'antd'

const baseURL = process.env.REACT_APP_API_URL || ''
const authorisationHeader = process.env.REACT_APP_AUTHORIZATION_HEADER

const fetchInitialConfig = {
    method: 'GET',
    headers: new Headers(),
}

const apiCall = async (url: string, params?: RequestInit) => {
    const responseParams = {
        ...fetchInitialConfig,
        ...params,
    }

    if (authorisationHeader) {
        if (responseParams.headers instanceof Headers) {
            responseParams.headers.set('Authorization', authorisationHeader)
        } else {
            responseParams.headers = new Headers({
                ...responseParams.headers,
                Authorization: authorisationHeader,
            })
        }
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
            } if (status > 200 && status < 300) {
                return true
            } else {
                await response.json().then((data: any) => {
                    throw new Error(data.message)
                })
                throw new Error('Something went wrong on API server!')
            }
        })
        .catch(error => {
            notification.error({ message: error.toString() })
        })
        .finally((result: any = false) => {
            return result
        })
}

export default apiCall
