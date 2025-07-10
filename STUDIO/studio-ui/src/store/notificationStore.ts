import { create } from 'zustand'
import { apiCall } from '../services'

interface NotificationStore {
    notification?: string
    loading?: boolean
    error?: any | null
    fetchNotification: () => Promise<void>
    setNotification: (notification: string) => Promise<void>
}

export const useNotificationStore = create<NotificationStore>((set) => ({
    notification: '',
    loading: false,
    error: null,
    fetchNotification: async () => {
        set({ loading: true, error: null })
        try {
            const notification = await apiCall('/public/notification.txt')
            set({ notification, loading: false })
        } catch (error) {
            set({ error, loading: false })
        }
    },
    setNotification: async (notification: string) => {
        set({ loading: true, error: null })
        try {
            await apiCall('/admin/notification.txt', {
                method: 'POST',
                body: notification
            })
            set({ notification, loading: false })
        } catch (error) {
            set({ error, loading: false })
        }
    }
}))