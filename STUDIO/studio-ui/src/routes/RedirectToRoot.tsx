import React, { useEffect } from 'react'
import { CONFIG } from '../services'

/**
 * Redirects to the site root with a full page reload.
 * Use for unknown routes so the server loads the legacy UI.
 */
export const RedirectToRoot: React.FC = () => {
    useEffect(() => {
        window.location.replace(CONFIG.CONTEXT + '/')
    }, [])
    return null
}
