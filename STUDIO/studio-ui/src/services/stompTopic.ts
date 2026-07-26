import { webSocketService, type WebSocketMessage } from './websocket'

export interface TopicSubscription {
    /** Cancel the subscription. Idempotent. */
    unsubscribe(): void
}

interface MultiplexedTopic {
    listeners: Set<(body: string) => void>
    stompSubscriptionId: string
    teardownTimer: ReturnType<typeof setTimeout> | undefined
}

/**
 * One STOMP subscription is held per destination and fan-out happens locally, so any number of
 * screens may watch the same topic for the price of one.
 *
 * <p>The teardown is deferred by a short cooldown after the last listener detaches. A panel reload
 * unsubscribes synchronously and re-subscribes a moment later — without the cooldown the listener
 * count would briefly hit zero, killing the STOMP subscription and dropping in-flight events.
 */
const topics = new Map<string, MultiplexedTopic>()
const TEARDOWN_COOLDOWN_MS = 5000

/**
 * Subscribe to a STOMP destination, sharing one server subscription between every local listener.
 *
 * The callback receives the raw message body; the caller parses it. STOMP only pushes transitions —
 * a subscriber needing the current state must fetch it separately first.
 */
export function subscribeTopic(destination: string, onBody: (body: string) => void): TopicSubscription {
    let entry = topics.get(destination)
    if (entry) {
        // A pending teardown from the last panel-reload tick — rescue it.
        if (entry.teardownTimer) {
            clearTimeout(entry.teardownTimer)
            entry.teardownTimer = undefined
        }
    } else {
        // Best-effort connect — `webSocketService.subscribe` queues subscriptions until
        // the STOMP client is connected, so we don't have to await this promise.
        void webSocketService.connect().catch(() => {
            // Connection failures surface via the service's own reconnect loop.
        })
        const newEntry: MultiplexedTopic = {
            listeners: new Set(),
            stompSubscriptionId: '',
            teardownTimer: undefined,
        }
        newEntry.stompSubscriptionId = webSocketService.subscribe(
            destination,
            (message: WebSocketMessage) => {
                newEntry.listeners.forEach(listener => listener(message.body))
            }
        )
        topics.set(destination, newEntry)
        entry = newEntry
    }
    entry.listeners.add(onBody)

    let unsubscribed = false
    return {
        unsubscribe: () => {
            if (unsubscribed) {
                return
            }
            unsubscribed = true
            entry.listeners.delete(onBody)
            if (entry.listeners.size === 0) {
                entry.teardownTimer = setTimeout(() => {
                    // Re-check size — a `subscribe()` could have arrived during the cooldown.
                    if (entry.listeners.size === 0) {
                        webSocketService.unsubscribe(entry.stompSubscriptionId)
                        topics.delete(destination)
                    }
                    entry.teardownTimer = undefined
                }, TEARDOWN_COOLDOWN_MS)
            }
        },
    }
}
