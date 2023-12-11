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
        .then(response => {
            if (response.status < 300) {
                return response.json()
            } else {
                throw new Error('Something went wrong on API server!')
            }
        })
        .catch(error => {
            notification.error({ message: error })
        })
}

export default apiCall
