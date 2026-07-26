import { useEffect, useRef } from 'react'

/**
 * Runs the callback when the user comes back to the tab — the window regains focus or the document
 * becomes visible again. The screens use it to re-read what a sleeping or backgrounded tab may have
 * kept stale past its trust window.
 *
 * The latest callback is always the one called, so callers may pass an inline closure.
 */
export function useWindowFocus(onFocus: () => void): void {
    const onFocusRef = useRef(onFocus)
    onFocusRef.current = onFocus

    useEffect(() => {
        const handler = () => {
            if (document.visibilityState === 'visible') {
                onFocusRef.current()
            }
        }
        window.addEventListener('focus', handler)
        document.addEventListener('visibilitychange', handler)
        return () => {
            window.removeEventListener('focus', handler)
            document.removeEventListener('visibilitychange', handler)
        }
    }, [])
}
