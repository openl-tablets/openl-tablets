import { Client, type IMessage, type StompConfig, type StompHeaders } from '@stomp/stompjs'

export interface WebSocketMessage {
    body: string
    headers: Record<string, string>
    command: string
    destination: string
}

/**
 * Told the subscription started, with the number the server gave that moment: what it publishes after reaches the
 * subscription. Matches the {@code status-sequence} header of {@code SubscriptionReceiptInterceptor}.
 */
export type SubscriptionStarted = (sequence: number) => void

/** The header of the receipt of a subscription that holds the number the subscription started at. */
const SEQUENCE_HEADER = 'status-sequence'

interface WebSocketSubscription {
    id: string
    destination: string
    callback: (message: WebSocketMessage) => void
    onStarted?: SubscriptionStarted | undefined
    stompSubscription?: { unsubscribe: () => void }
}

export class WebSocketConnectionTimeoutError extends Error {
    constructor(timeoutMs: number) {
        super(`WebSocket connection timeout after ${timeoutMs}ms`)
        this.name = 'WebSocketConnectionTimeoutError'
    }
}

/** A random token for a subscription id, drawn from the browser's cryptographic source. */
function randomSuffix(): string {
    const bytes = crypto.getRandomValues(new Uint8Array(5))
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
}

class WebSocketService {
    private client: Client | null = null
    private readonly subscriptions: Map<string, WebSocketSubscription> = new Map()
    private isConnected = false
    private reconnectAttempts = 0
    /** Counts the receipts asked for, so that each one is told apart. */
    private receipts = 0
    private readonly maxReconnectAttempts = 5
    private readonly reconnectDelay = 5000

    constructor() {
        this.initializeClient()
    }

    private initializeClient() {
        const url = new URL(document.baseURI)
        const proto = url.protocol === 'https:' ? 'wss:' : 'ws:'
        // The handshake has an address of its own, apart from the REST API; url.pathname already ends in a slash.
        const wsUrl = `${proto}//${url.host}${url.pathname}ws`

        const stompConfig: StompConfig = {
            brokerURL: wsUrl,
            reconnectDelay: this.reconnectDelay,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: this.onConnect.bind(this),
            onDisconnect: this.onDisconnect.bind(this),
            onStompError: this.onError.bind(this),
        }

        this.client = new Client(stompConfig)
    }

    private onConnect() {
        this.isConnected = true
        this.reconnectAttempts = 0

        // Re-subscribe to all previous subscriptions. A subscription that is told when it starts is told again:
        // nothing published while the connection was down arrives later.
        this.subscriptions.forEach(subscription => this.start(subscription))
    }

    /**
     * Sends the subscription to the server. One that wants to know when it starts asks for a receipt, which the
     * server sends once the subscription is registered.
     */
    private start(subscription: WebSocketSubscription) {
        if (!this.client) {
            return
        }
        const { id, onStarted } = subscription
        let headers: StompHeaders = { id }
        if (onStarted) {
            this.receipts += 1
            const receipt = `${id}-${this.receipts}`
            // Watched before the frame leaves, so the receipt cannot arrive unwatched.
            this.client.watchForReceipt(receipt, frame => {
                const sequence = Number(frame.headers[SEQUENCE_HEADER])
                if (Number.isSafeInteger(sequence)) {
                    onStarted(sequence)
                }
            })
            headers = { id, receipt }
        }
        subscription.stompSubscription = this.client.subscribe(subscription.destination, (message: IMessage) => {
            subscription.callback({
                body: message.body,
                headers: message.headers,
                command: message.command,
                // The subscription destination rather than the one the message names
                destination: subscription.destination,
            })
        }, headers)
    }

    private onDisconnect() {
        this.isConnected = false
    }

    private onError() {
        this.isConnected = false

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            setTimeout(() => {
                this.connect().catch(() => {
                    // Reconnect failures are expected during transient outages; the next
                    // onStompError will schedule another attempt until the cap is reached.
                })
            }, this.reconnectDelay * this.reconnectAttempts)
        }
    }

    public connect(timeoutMs = 8000): Promise<void> {
        return new Promise((resolve, reject) => {
            if (!this.client) {
                reject(new Error('WebSocket client not initialized'))
                return
            }

            if (this.isConnected) {
                resolve()
                return
            }

            this.client.activate()

            const deadline = Date.now() + timeoutMs
            let settled = false

            // Wait for connection with timeout protection
            const checkConnection = () => {
                if (settled) {
                    return
                }
                if (this.isConnected) {
                    settled = true
                    resolve()
                } else if (Date.now() >= deadline) {
                    settled = true
                    // Timeout should terminate this connect attempt and prevent late connect state.
                    void this.client?.deactivate()
                    reject(new WebSocketConnectionTimeoutError(timeoutMs))
                } else {
                    setTimeout(checkConnection, 100)
                }
            }
            checkConnection()
        })
    }

    public disconnect() {
        if (this.client && this.isConnected) {
            void this.client.deactivate()
        }
    }

    /**
     * Subscribes to the destination, now or as soon as the client connects, and again after every reconnect.
     *
     * @param onStarted told every time the subscription starts on the server, with the number of that moment
     * @returns the id of the subscription, for {@link unsubscribe}
     */
    public subscribe(
        destination: string,
        callback: (message: WebSocketMessage) => void,
        subscriptionId?: string,
        onStarted?: SubscriptionStarted
    ): string {
        const id = subscriptionId || `sub_${Date.now()}_${randomSuffix()}`

        const subscription: WebSocketSubscription = {
            id,
            destination,
            callback,
            onStarted,
        }

        this.subscriptions.set(id, subscription)

        if (this.isConnected) {
            this.start(subscription)
        }

        return id
    }

    public unsubscribe(subscriptionId: string) {
        const subscription = this.subscriptions.get(subscriptionId)
        if (subscription) {
            // Use the stored STOMP subscription's unsubscribe method
            if (subscription.stompSubscription) {
                subscription.stompSubscription.unsubscribe()
            }
            this.subscriptions.delete(subscriptionId)
        }
    }

    public send(destination: string, body: string, headers?: Record<string, string>) {
        if (!this.client || !this.isConnected) {
            return
        }

        this.client.publish({
            destination,
            body,
            ...(headers !== undefined && { headers })
        })
    }

    public getConnectionStatus(): boolean {
        return this.isConnected
    }

    public getSubscriptions(): WebSocketSubscription[] {
        return Array.from(this.subscriptions.values())
    }
}

// Create singleton instance
export const webSocketService = new WebSocketService()
export default webSocketService
