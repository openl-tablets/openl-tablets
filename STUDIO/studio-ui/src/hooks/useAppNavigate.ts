import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

/**
 * Navigates to another screen of the app.
 *
 * <p>Every screen is the app's own, so the router swaps the content in place — no page load, and the
 * scripts, the styles and the WebSocket connection stay as they are.
 *
 * <p>Takes router paths, without the servlet context — the same values {@link useNavigate} takes.
 */
export const useAppNavigate = () => {
    const navigate = useNavigate()
    return useCallback((to: string) => void navigate(to), [navigate])
}
