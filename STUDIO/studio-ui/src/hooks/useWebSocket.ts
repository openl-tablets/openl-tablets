import { useEffect, useRef, useState, useCallback } from 'react'
import { webSocketService, WebSocketMessage } from '../services/websocket'

export interface UseWebSocketOptions {
    autoConnect?: boolean
    onConnect?: () => void
    onDisconnect?: () => void
    onError?: (error: any) => void
}

export interface UseWebSocketReturn {
    isConnected: boolean
    connect: () => Promise<void>
    disconnect: () => void
    subscribe: (destination: string, callback: (message: WebSocketMessage) => void, subscriptionId?: string) => string
    unsubscribe: (subscriptionId: string) => void
    send: (destination: string, body: string, headers?: { [key: string]: string }) => void
    subscriptions: string[]
}

export const useWebSocket = (options: UseWebSocketOptions = {}): UseWebSocketReturn => {
    const {
        autoConnect = true,
        onConnect,
        onDisconnect,
        onError
    } = options

    const [isConnected, setIsConnected] = useState(false)
    const [subscriptions, setSubscriptions] = useState<string[]>([])
    const subscriptionsRef = useRef<Set<string>>(new Set())

    const connect = useCallback(async () => {
        try {
            await webSocketService.connect()
            setIsConnected(true)
            onConnect?.()
        } catch (error) {
            console.error('Failed to connect to WebSocket:', error)
            onError?.(error)
        }
    }, [onConnect, onError])

    const disconnect = useCallback(() => {
        webSocketService.disconnect()
        setIsConnected(false)
        onDisconnect?.()
    }, [onDisconnect])

    const subscribe = useCallback((
        destination: string, 
        callback: (message: WebSocketMessage) => void, 
        subscriptionId?: string
    ) => {
        const id = webSocketService.subscribe(destination, callback, subscriptionId)
        subscriptionsRef.current.add(id)
        setSubscriptions(Array.from(subscriptionsRef.current))
        return id
    }, [])

    const unsubscribe = useCallback((subscriptionId: string) => {
        webSocketService.unsubscribe(subscriptionId)
        subscriptionsRef.current.delete(subscriptionId)
        setSubscriptions(Array.from(subscriptionsRef.current))
    }, [])

    const send = useCallback((
        destination: string, 
        body: string, 
        headers?: { [key: string]: string }
    ) => {
        webSocketService.send(destination, body, headers)
    }, [])

    // Auto-connect on mount
    useEffect(() => {
        if (autoConnect) {
            connect()
        }

        // Cleanup on unmount
        return () => {
            // Unsubscribe from all subscriptions
            subscriptionsRef.current.forEach(id => {
                webSocketService.unsubscribe(id)
            })
            subscriptionsRef.current.clear()
            setSubscriptions([])
        }
    }, [autoConnect, connect])

    // Monitor connection status
    useEffect(() => {
        const checkConnection = () => {
            const connected = webSocketService.getConnectionStatus()
            if (connected !== isConnected) {
                setIsConnected(connected)
                if (connected) {
                    onConnect?.()
                } else {
                    onDisconnect?.()
                }
            }
        }

        const interval = setInterval(checkConnection, 1000)
        return () => clearInterval(interval)
    }, [isConnected, onConnect, onDisconnect])

    return {
        isConnected,
        connect,
        disconnect,
        subscribe,
        unsubscribe,
        send,
        subscriptions
    }
}

export default useWebSocket
