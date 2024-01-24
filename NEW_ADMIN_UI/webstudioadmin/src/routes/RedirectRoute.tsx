import React from 'react'
import { useParams } from 'react-router-dom'
import { NoMatch } from './NoMatch'

export const RedirectRoute = () => {
    const { page } = useParams()

    if (page === 'editor') {
        window.location.href = '/'
        return null
    } else if (page === 'repository') {
        window.location.href = '/faces/pages/modules/repository/index.xhtml'
        return null
    } else {
        return <NoMatch />
    }
}