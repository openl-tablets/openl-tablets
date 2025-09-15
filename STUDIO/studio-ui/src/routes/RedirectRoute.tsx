import React from 'react'
import { useParams } from 'react-router-dom'
import NotFound from '../pages/404'
import { CONFIG } from '../services'

interface RedirectRouteProps {
    to?: string
}

export const RedirectRoute: React.FC<RedirectRouteProps> = ({ to }) => {
    const { page } = useParams()

    if (page === 'editor') {
        window.location.href = CONFIG.CONTEXT + '/'
        return null
    } else if (to === 'logout') {
        window.location.href = CONFIG.CONTEXT + '/logout'
        return null

    } else {
        return <NotFound />
    }
}
