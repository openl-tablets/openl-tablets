import { renderHook, act, waitFor } from '@testing-library/react'
import { useWebSocket } from 'hooks/useWebSocket'
import { webSocketService } from 'services/websocket'

describe('useWebSocket', () => {
    const connectSpy = jest.spyOn(webSocketService, 'connect')
    const disconnectSpy = jest.spyOn(webSocketService, 'disconnect')
    const subscribeSpy = jest.spyOn(webSocketService, 'subscribe')
    const unsubscribeSpy = jest.spyOn(webSocketService, 'unsubscribe')
    const sendSpy = jest.spyOn(webSocketService, 'send')
    const getConnectionStatusSpy = jest.spyOn(webSocketService, 'getConnectionStatus')

    beforeEach(() => {
        jest.clearAllMocks()
        connectSpy.mockResolvedValue()
        getConnectionStatusSpy.mockReturnValue(false)
        // jest-fail-on-console re-wraps console.error in its own beforeEach, so the
        // spy must be re-installed per test (after that wrap runs) rather than once
        // at describe level.
        jest.spyOn(console, 'error').mockImplementation(() => {})
    })

    afterAll(() => {
        connectSpy.mockRestore()
        disconnectSpy.mockRestore()
        subscribeSpy.mockRestore()
        unsubscribeSpy.mockRestore()
        sendSpy.mockRestore()
        getConnectionStatusSpy.mockRestore()
    })

    it('swallows autoConnect rejection and calls onError callback', async () => {
        const error = new Error('connect failed')
        const onError = jest.fn()
        connectSpy.mockRejectedValueOnce(error)

        renderHook(() => useWebSocket({ autoConnect: true, onError }))

        await waitFor(() => {
            expect(onError).toHaveBeenCalledWith(error)
        })
    })

    it('rethrows errors for manual connect calls', async () => {
        const error = new Error('manual connect failed')
        connectSpy.mockRejectedValueOnce(error)

        const { result } = renderHook(() => useWebSocket({ autoConnect: false }))

        await expect(result.current.connect(100)).rejects.toThrow('manual connect failed')
    })

    it('tracks subscriptions on subscribe/unsubscribe', () => {
        subscribeSpy.mockReturnValue('sub-1')

        const { result } = renderHook(() => useWebSocket({ autoConnect: false }))

        act(() => {
            result.current.subscribe('/topic/test', jest.fn(), 'sub-1')
        })
        expect(result.current.subscriptions).toEqual(['sub-1'])

        act(() => {
            result.current.unsubscribe('sub-1')
        })
        expect(result.current.subscriptions).toEqual([])
    })

    it('delegates send and disconnect to webSocketService', () => {
        const { result } = renderHook(() => useWebSocket({ autoConnect: false }))

        act(() => {
            result.current.send('/topic/send', '{"ok":true}')
            result.current.disconnect()
        })

        expect(sendSpy).toHaveBeenCalledWith('/topic/send', '{"ok":true}', undefined)
        expect(disconnectSpy).toHaveBeenCalled()
    })
})
