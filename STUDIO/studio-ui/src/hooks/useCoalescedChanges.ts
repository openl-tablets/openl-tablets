import { useEffect, useRef, type DependencyList } from 'react'
import { WORKSPACE_CHANGED_EVENT } from '../services/apiCall'
import { isOwnEcho, type ChangePing } from '../services/changePing'
import type { TopicSubscription } from '../services/stompTopic'

/** A run of pings within this window collapses into one refresh. */
const COALESCE_MS = 500

/**
 * How long after the user's own mutation a ping still reads as its echo. The server debounces its
 * pings by a second, so the echo of an action lands within roughly a second and a half of it.
 */
const ECHO_WINDOW_MS = 2500

/**
 * The longest a gathered batch may wait, whatever it is waiting for. A user acting more often than
 * the echo window, or an action whose request never comes back, would otherwise postpone the
 * delivery forever — starving exactly the other-session changes the wait exists to preserve.
 */
const MAX_HOLD_MS = 10_000

// The screens reload themselves right after the user's own action, so the ping the backend sends
// about that same action would only repeat the reload. Every successful mutating call announces
// itself on this browser event; a ping close on its heels is likely that echo.
let lastOwnMutationAt = 0
window.addEventListener(WORKSPACE_CHANGED_EVENT, () => {
    lastOwnMutationAt = Date.now()
})

/**
 * The shared shape of the live-refresh hooks: subscribe to change pings for the component's whole
 * life, collapse bursts into one call carrying everything the window gathered, and always call the
 * latest callback so callers may pass an inline closure.
 *
 * A ping this tab caused itself is dropped at the door — but only while the screen is known to have
 * just reloaded on that action of its own. A mutation whose answer never reached the browser (a
 * failure after the write, a dropped connection) leaves no such mark, so its ping is delivered and
 * the screen still catches up. The ping names its origins, so a change of another session is never
 * dropped with the echo.
 *
 * A ping naming no origin at all was made outside a request — the files watcher, a repository poll,
 * a commit anyone can see — and may still be this tab's own action reaching the disk. Such a batch
 * is held until the echo window of the last own mutation passes and delivered then, so the screens
 * run one quiet refresh instead of an immediate duplicate.
 *
 * While {@code hold} is true the caller is running an action of its own, and the batch waits for it.
 * The screen must show the answer to the action the user is waiting for, and a refresh started
 * beside it would supersede that answer and leave the page on its pre-action state. Nothing is ever
 * dropped for waiting: a batch is delivered once the wait ends, and in any case within
 * {@link MAX_HOLD_MS}.
 *
 * The subscription is renewed when {@code deps} change, and skipped entirely while {@code subscribe}
 * is {@code null} — e.g. before the screen knows what to watch. A batch gathered by the previous
 * subscription travels to the new one only when an action of the screen is running: an action is
 * what renews a subscription without changing what the screen watches (opening a project changes its
 * id), and the changes of others gathered meanwhile are still owed to the user. Renewed for any
 * other reason the screen is watching something else — another project — and the batch goes with the
 * subscription that gathered it.
 */
export function useCoalescedChanges(
    subscribe: ((onPing: (ping: ChangePing) => void) => TopicSubscription) | null,
    onChange: (pings: ChangePing[]) => void,
    deps: DependencyList,
    hold = false
): void {
    const onChangeRef = useRef(onChange)
    onChangeRef.current = onChange
    const subscribeRef = useRef(subscribe)
    subscribeRef.current = subscribe
    const holdRef = useRef(hold)
    holdRef.current = hold
    // Outside the subscription effect: a renewed subscription continues the batch instead of
    // throwing away what the previous one gathered.
    const gathered = useRef<ChangePing[]>([])
    // When the pending batch started waiting; caps the wait so it cannot be extended forever.
    const heldSince = useRef(0)

    useEffect(() => {
        const start = subscribeRef.current
        if (!start) {
            return
        }
        let timer: ReturnType<typeof setTimeout> | undefined
        const deliver = () => {
            timer = undefined
            const capped = Date.now() - heldSince.current >= MAX_HOLD_MS
            // An action of the caller's own is in flight; ask again when it may have finished.
            if (holdRef.current && !capped) {
                timer = setTimeout(deliver, COALESCE_MS)
                return
            }
            // A batch every ping of which names an origin needs no wait: this tab's own echo was
            // dropped at the door, so what is left is somebody else's change and goes through at
            // once. An unattributed ping keeps the old rule — hold it out of the echo window.
            const unattributed = gathered.current.some(ping => ping.origins.length === 0)
            const wait = ECHO_WINDOW_MS - (Date.now() - lastOwnMutationAt)
            if (unattributed && wait > 0 && !capped) {
                timer = setTimeout(deliver, wait)
                return
            }
            const pings = gathered.current
            gathered.current = []
            onChangeRef.current(pings)
        }
        const gather = (ping: ChangePing) => {
            if (gathered.current.length === 0) {
                heldSince.current = Date.now()
            }
            gathered.current.push(ping)
            timer ??= setTimeout(deliver, COALESCE_MS)
        }
        const subscription = start(ping => {
            // The echo of an action this screen has already reloaded on. Without a mutation of this
            // tab behind it the ping is the only word the screen gets, so it is kept.
            if (isOwnEcho(ping) && Date.now() - lastOwnMutationAt < ECHO_WINDOW_MS) {
                return
            }
            gather(ping)
        })
        if (gathered.current.length > 0) {
            // Handed over by the previous subscription, with nothing to wake it up on its own.
            timer = setTimeout(deliver, COALESCE_MS)
        }
        return () => {
            if (timer) {
                clearTimeout(timer)
            }
            if (!holdRef.current) {
                // Watching something else now — the batch belongs to what was watched before.
                gathered.current = []
            }
            subscription.unsubscribe()
        }
        // The subscription target is described by the caller's deps; the callbacks ride on refs.

    }, deps)
}
