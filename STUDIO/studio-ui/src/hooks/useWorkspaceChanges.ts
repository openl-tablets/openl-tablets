import { subscribeWorkspaceChanges } from '../services/workspaceChanges'
import { useCoalescedChanges } from './useCoalescedChanges'

/**
 * Runs the callback whenever the backend pings that the projects a screen shows may be stale —
 * the user changed their workspace (from any session), or a design repository got new content.
 *
 * The subscription lives for the component's whole life; the latest callback is always the one
 * called, so callers may pass an inline closure. Bursts of pings collapse into one call.
 */
export function useWorkspaceChanges(onChange: () => void): void {
    useCoalescedChanges<void>(subscribeWorkspaceChanges, onChange, [])
}
