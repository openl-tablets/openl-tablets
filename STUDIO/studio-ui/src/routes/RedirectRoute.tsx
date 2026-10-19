import React, { useEffect } from 'react'
import { useParams } from 'react-router'
import NotFound from '../pages/404'
import { CONFIG } from '../services'

interface RedirectRouteProps {
    to?: string
}

/** The address below the context a route leaves the application for, or nothing for a page it does not know. */
const leaveFor = (page?: string, to?: string) => {
    if (page === 'editor') {
        return '/'
    }
    if (to === 'logout') {
        return '/logout'
    }
    return undefined
}

export const RedirectRoute: React.FC<RedirectRouteProps> = ({ to }) => {
    const { page } = useParams()
    const target = leaveFor(page, to)

    useEffect(() => {
        if (target) {
            window.location.href = CONFIG.CONTEXT + target
        }
    }, [target])

    return target ? null : <NotFound />
}
