import React from 'react'
import { useParams } from 'react-router-dom'
import { NoMatch } from './NoMatch'

const baseRoot = process.env.BASE_PATH || ''

export const RedirectRoute = () => {
    const { page } = useParams()

    if (page === 'editor') {
        window.location.href = baseRoot + '/'
        return null
    } else if (page === 'login') {
        window.location.href = baseRoot + '/faces/pages/login.xhtml'
        return null
    } else {
        return <NoMatch />
    }
}