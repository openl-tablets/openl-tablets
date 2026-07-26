import { useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { CONFIG } from '../services'

/** The pages the server renders whole — the legacy JSF Editor at the root and everything under `faces/`. */
const isServerPage = (path: string): boolean => path === '/' || path.startsWith('/faces/')

/**
 * True when going from `from` to `to` must load a whole document: one of the two is a legacy
 * server-rendered page, so the app cannot just swap the screen in place.
 */
export const needsDocumentLoad = (from: string, to: string): boolean => isServerPage(from) || isServerPage(to)

/**
 * Navigates to another screen of the app.
 *
 * Between the app's own screens the router swaps the content in place — no page load, so the
 * scripts, styles and the WebSocket connection stay. The legacy pages (the Editor at the root,
 * everything under `faces/`) come from the server as whole documents, so going to one — or leaving
 * one — loads the page anew.
 *
 * Takes router paths, without the servlet context — the same values {@link useNavigate} takes.
 */
export const useAppNavigate = () => {
    const navigate = useNavigate()
    const { pathname } = useLocation()
    return useCallback((to: string) => {
        if (needsDocumentLoad(pathname, to)) {
            window.location.href = CONFIG.CONTEXT + to
        } else {
            void navigate(to)
        }
    }, [navigate, pathname])
}
