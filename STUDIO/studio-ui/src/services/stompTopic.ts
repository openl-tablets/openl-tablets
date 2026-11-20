import { webSocketService, type SubscriptionStarted, type WebSocketMessage } from './websocket'

export interface TopicSubscription {
    /** Cancel the subscription. Idempotent. */
    unsubscribe(): void
}

interface TopicListener {
    onBody: (body: string) => void
    onStarted?: SubscriptionStarted | undefined
}

interface MultiplexedTopic {
    listeners: Set<TopicListener>
    stompSubscriptionId: string
    teardownTimer: ReturnType<typeof setTimeout> | undefined
    /** The number the server subscription last started at; null until it started. */
    startedAt: number | null
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
 * The callback receives the raw message body; the caller parses it. STOMP only pushes transitions — a
 * subscriber needing the current state reads it separately. {@code onStarted} tells it the number the server
 * subscription started at, as soon as it is known and after every reconnect: a state read under a smaller number
 * may have changed before anything was pushed to the subscription, and is read again. A listener joining a
 * subscription that already started is told its number at once.
 */
export function subscribeTopic(
    destination: string,
    onBody: (body: string) => void,
    onStarted?: SubscriptionStarted
): TopicSubscription {
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
            startedAt: null,
        }
        newEntry.stompSubscriptionId = webSocketService.subscribe(
            destination,
            (message: WebSocketMessage) => {
                newEntry.listeners.forEach(listener => listener.onBody(message.body))
            },
            undefined,
            sequence => {
                newEntry.startedAt = sequence
                newEntry.listeners.forEach(listener => listener.onStarted?.(sequence))
            }
        )
        topics.set(destination, newEntry)
        entry = newEntry
    }
    let unsubscribed = false
    const listener: TopicListener = { onBody, onStarted }
    entry.listeners.add(listener)
    const { startedAt } = entry
    if (onStarted && startedAt !== null) {
        // Told asynchronously, as it is told while the subscription is still starting.
        queueMicrotask(() => {
            if (!unsubscribed) {
                onStarted(startedAt)
            }
        })
    }

    return {
        unsubscribe: () => {
            if (unsubscribed) {
                return
            }
            unsubscribed = true
            entry.listeners.delete(listener)
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
