import { subscribeWorkspaceChanges } from '../services/workspaceChanges'
import { useCoalescedChanges } from './useCoalescedChanges'

/**
 * Runs the callback whenever the backend pings that the projects a screen shows may be stale —
 * the user changed their workspace (from any session), or a design repository got new content.
 *
 * The subscription lives for the component's whole life; the latest callback is always the one
 * called, so callers may pass an inline closure. Bursts of pings collapse into one call, and a ping
 * this tab caused itself never arrives — it has already read the change. With {@code holdWhile} the
 * screen tells the hook it is running an action of its own, and the refresh waits for it instead of
 * racing the read the user is waiting for.
 */
export function useWorkspaceChanges(onChange: () => void, { holdWhile = false }: { holdWhile?: boolean } = {}): void {
    useCoalescedChanges(subscribeWorkspaceChanges, () => onChange(), [], holdWhile)
}
