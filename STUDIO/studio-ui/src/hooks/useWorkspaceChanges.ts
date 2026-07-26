import { useEffect, useRef } from 'react'
import { subscribeWorkspaceChanges } from '../services/workspaceChanges'

/** A run of pings within this window collapses into one refresh. */
const COALESCE_MS = 500

/**
 * Runs the callback whenever the backend pings that the projects a screen shows may be stale —
 * the user changed their workspace (from any session), or a design repository got new content.
 *
 * The subscription lives for the component's whole life; the latest callback is always the one
 * called, so callers may pass an inline closure. Bursts of pings collapse into one call.
 */
export function useWorkspaceChanges(onChange: () => void): void {
    const onChangeRef = useRef(onChange)
    onChangeRef.current = onChange

    useEffect(() => {
        let timer: ReturnType<typeof setTimeout> | undefined
        const subscription = subscribeWorkspaceChanges(() => {
            timer ??= setTimeout(() => {
                timer = undefined
                onChangeRef.current()
            }, COALESCE_MS)
        })
        return () => {
            if (timer) {
                clearTimeout(timer)
            }
            subscription.unsubscribe()
        }
    }, [])
}
