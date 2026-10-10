import { webSocketService } from '../services/websocket'
import { useNotificationStore } from './notificationStore'

vi.mock('../services/websocket', () => ({
    webSocketService: { send: vi.fn(), connect: vi.fn(), subscribe: vi.fn(), getSubscriptions: vi.fn(() => []) },
}))

describe('notificationStore', () => {
    it('leaves the notification shown to the answer of the server', () => {
        useNotificationStore.setState({ isWebSocketConnected: true, notification: 'Old news' })

        useNotificationStore.getState().setNotification('   ')

        expect(webSocketService.send).toHaveBeenCalledWith('/app/admin/notification.txt', '   ')
        // The server clears a message of blanks and tells every screen so, this one included.
        expect(useNotificationStore.getState().notification).toBe('Old news')
    })
})
