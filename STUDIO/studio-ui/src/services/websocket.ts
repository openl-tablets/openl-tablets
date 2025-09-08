import { Client, IMessage, StompConfig } from '@stomp/stompjs'

export interface WebSocketMessage {
    body: string
    headers: { [key: string]: string }
    command: string
    destination: string
}

export interface WebSocketSubscription {
    id: string
    destination: string
    callback: (message: WebSocketMessage) => void
}

class WebSocketService {
    private client: Client | null = null
    private subscriptions: Map<string, WebSocketSubscription> = new Map()
    private isConnected = false
    private reconnectAttempts = 0
    private maxReconnectAttempts = 5
    private reconnectDelay = 5000

    constructor() {
        this.initializeClient()
    }

    private initializeClient() {
        const url = new URL(document.baseURI)
        const proto = url.protocol === 'https:' ? 'wss:' : 'ws:'
        const wsUrl = `${proto}//${url.host}${url.pathname}web/ws`
        
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
        
        // Re-subscribe to all previous subscriptions
        this.subscriptions.forEach((subscription) => {
            this.subscribe(subscription.destination, subscription.callback, subscription.id)
        })
    }

    private onDisconnect() {
        this.isConnected = false
    }

    private onError(error: any) {
        console.error('WebSocket Error:', error)
        this.isConnected = false
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            setTimeout(() => {
                this.connect()
            }, this.reconnectDelay * this.reconnectAttempts)
        } else {
            console.error('Max reconnection attempts reached')
        }
    }

    public connect(): Promise<void> {
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
            
            // Wait for connection
            const checkConnection = () => {
                if (this.isConnected) {
                    resolve()
                } else {
                    setTimeout(checkConnection, 100)
                }
            }
            checkConnection()
        })
    }

    public disconnect() {
        if (this.client && this.isConnected) {
            this.client.deactivate()
        }
    }

    public subscribe(
        destination: string, 
        callback: (message: WebSocketMessage) => void, 
        subscriptionId?: string
    ): string {
        if (!this.client || !this.isConnected) {
            console.warn('WebSocket not connected, subscription will be queued')
        }

        const id = subscriptionId || `sub_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        
        const subscription: WebSocketSubscription = {
            id,
            destination,
            callback
        }

        this.subscriptions.set(id, subscription)

        if (this.client && this.isConnected) {
            this.client.subscribe(destination, (message: IMessage) => {
                const wsMessage: WebSocketMessage = {
                    body: message.body,
                    headers: message.headers,
                    command: message.command,
                    destination: destination // Use the subscription destination instead of message.destination
                }
                callback(wsMessage)
            })
        }

        return id
    }

    public unsubscribe(subscriptionId: string) {
        const subscription = this.subscriptions.get(subscriptionId)
        if (subscription && this.client && this.isConnected) {
            this.client.unsubscribe(subscriptionId)
        }
        this.subscriptions.delete(subscriptionId)
    }

    public send(destination: string, body: string, headers?: { [key: string]: string }) {
        if (!this.client || !this.isConnected) {
            console.error('WebSocket not connected, cannot send message')
            return
        }

        this.client.publish({
            destination,
            body,
            headers
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
