import React, { useContext } from 'react'
import { useParams } from 'react-router-dom'
import NotFound from '../pages/404'
import { SystemContext } from '../contexts'
import { CONFIG } from '../services'

interface RedirectRouteProps {
    to?: string
}

export const RedirectRoute: React.FC<RedirectRouteProps> = ({ to }) => {
    const { page } = useParams()
    const { getLogoutUrl } = useContext(SystemContext)

    if (page === 'editor') {
        window.location.href = CONFIG.CONTEXT + '/'
        return null
    } else if (to === 'logout') {
        window.location.href = CONFIG.CONTEXT + getLogoutUrl()
        return null

    } else {
        return <NotFound />
    }
}
