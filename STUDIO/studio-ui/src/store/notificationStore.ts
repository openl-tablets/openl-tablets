import { create } from 'zustand'
import { webSocketService, WebSocketMessage } from '../services/websocket'

interface NotificationStore {
    notification?: string
    loading?: boolean
    error?: unknown
    isWebSocketConnected?: boolean
    setNotification: (notification: string) => void
    initializeWebSocket: () => void
    cleanupWebSocket: () => void
}

export const useNotificationStore = create<NotificationStore>((set, get) => ({
    notification: '',
    loading: false,
    error: null,
    isWebSocketConnected: false,
    setNotification: (notification: string = '1') => {
        const { isWebSocketConnected } = get()

        if (isWebSocketConnected) {
            // The server answers every screen with the notification as it keeps it, this one included: a message
            // of blanks, for one, clears it.
            webSocketService.send('/app/admin/notification.txt', notification)
        }
    },
    initializeWebSocket: () => {
        const { isWebSocketConnected } = get()

        if (isWebSocketConnected) {
            return // Already initialized
        }

        // Connect to WebSocket
        webSocketService.connect().then(() => {
            set({ isWebSocketConnected: true })

            // Subscribe to notification topics
            webSocketService.subscribe('/app/public/notification.txt', (message: WebSocketMessage) => {
                set({ notification: message.body })
            })

            webSocketService.subscribe('/topic/public/notification.txt', (message: WebSocketMessage) => {
                set({ notification: message.body })
            })

            webSocketService.subscribe('/user/queue/errors', (message: WebSocketMessage) => {
                set({ error: message.body })
            })

        }).catch((error) => {
            set({ error, isWebSocketConnected: false })
        })
    },
    cleanupWebSocket: () => {
        const { isWebSocketConnected } = get()

        if (isWebSocketConnected) {
            // Unsubscribe from all notification topics
            const subscriptions = webSocketService.getSubscriptions()
            subscriptions.forEach(sub => {
                if (sub.destination.includes('notification') || sub.destination.includes('errors')) {
                    webSocketService.unsubscribe(sub.id)
                }
            })

            set({ isWebSocketConnected: false })
        }
    }
}))
