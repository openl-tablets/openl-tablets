import { useEffect, useRef, type DependencyList } from 'react'
import { WORKSPACE_CHANGED_EVENT } from '../services/apiCall'
import type { TopicSubscription } from '../services/stompTopic'

/** A run of pings within this window collapses into one refresh. */
const COALESCE_MS = 500

/**
 * How long after the user's own mutation a ping still reads as its echo. The server debounces its
 * pings by a second, so the echo of an action lands within roughly a second and a half of it.
 */
const ECHO_WINDOW_MS = 2500

/**
 * The longest a gathered batch may wait out echo windows. A user acting more often than the echo
 * window would otherwise postpone the delivery forever — starving exactly the other-user changes
 * the hold exists to preserve.
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
 * A ping arriving right after the user's own mutation is likely its echo — the screen has already
 * reloaded with data at least as fresh as what the ping stands for. Such a batch is not dropped
 * (someone else's real change can hide behind the echo): it is held until the echo window passes
 * and delivered then, so the screens run one quiet refresh instead of an immediate duplicate.
 *
 * The subscription is renewed when {@code deps} change, and skipped entirely while {@code subscribe}
 * is {@code null} — e.g. before the screen knows what to watch.
 */
export function useCoalescedChanges<T = void>(
    subscribe: ((onPing: (payload: T) => void) => TopicSubscription) | null,
    onChange: (payloads: T[]) => void,
    deps: DependencyList
): void {
    const onChangeRef = useRef(onChange)
    onChangeRef.current = onChange
    const subscribeRef = useRef(subscribe)
    subscribeRef.current = subscribe

    useEffect(() => {
        const start = subscribeRef.current
        if (!start) {
            return
        }
        let timer: ReturnType<typeof setTimeout> | undefined
        let gathered: T[] = []
        // When the pending batch started waiting; caps the hold so it cannot be extended forever.
        let heldSince = 0
        const deliver = () => {
            timer = undefined
            // Still inside the echo window of the user's own action: hold the batch (it keeps
            // gathering) and deliver once the window has passed — but never longer than the cap,
            // or a user acting continuously would starve the delivery.
            const wait = ECHO_WINDOW_MS - (Date.now() - lastOwnMutationAt)
            if (wait > 0 && Date.now() - heldSince < MAX_HOLD_MS) {
                timer = setTimeout(deliver, wait)
                return
            }
            const payloads = gathered
            gathered = []
            onChangeRef.current(payloads)
        }
        const subscription = start(payload => {
            if (gathered.length === 0) {
                heldSince = Date.now()
            }
            gathered.push(payload)
            timer ??= setTimeout(deliver, COALESCE_MS)
        })
        return () => {
            if (timer) {
                clearTimeout(timer)
            }
            subscription.unsubscribe()
        }
        // The subscription target is described by the caller's deps; the callbacks ride on refs.

    }, deps)
}
