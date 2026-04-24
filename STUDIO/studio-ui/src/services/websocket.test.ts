describe('webSocketService', () => {
    const hadOwnBaseUri = Object.prototype.hasOwnProperty.call(document, 'baseURI')
    const originalBaseUriDescriptor = Object.getOwnPropertyDescriptor(document, 'baseURI')

    beforeEach(() => {
        vi.resetModules()
        vi.useFakeTimers()
        vi.clearAllMocks()
        Object.defineProperty(document, 'baseURI', {
            configurable: true,
            value: 'https://localhost:8080/webstudio/',
        })
    })

    afterEach(() => {
        vi.useRealTimers()
        if (hadOwnBaseUri && originalBaseUriDescriptor) {
            Object.defineProperty(document, 'baseURI', originalBaseUriDescriptor)
        } else {
            // Remove test override and fall back to prototype-provided baseURI.
            delete (document as { baseURI?: string }).baseURI
        }
    })

    const loadService = async () => {
        let latestConfig: any
        let latestClient: any

        vi.doMock('@stomp/stompjs', () => {
            class MockClient {
                config: any
                activate = vi.fn()
                deactivate = vi.fn()
                subscribe = vi.fn(() => ({ unsubscribe: vi.fn() }))
                publish = vi.fn()

                constructor(config: any) {
                    this.config = config
                    latestConfig = config
                    latestClient = this
                }
            }

            return {
                Client: MockClient,
            }
        })

        const mod = await import('services/websocket')
        const webSocketService = mod.webSocketService
        const WebSocketConnectionTimeoutError = mod.WebSocketConnectionTimeoutError

        return {
            webSocketService,
            WebSocketConnectionTimeoutError,
            latestConfig: () => latestConfig,
            latestClient: () => latestClient,
        }
    }

    it('builds websocket URL from base URI', async () => {
        const { latestConfig } = await loadService()

        expect(latestConfig().brokerURL).toBe('wss://localhost:8080/webstudio/web/ws')
    })

    it('rejects connect with timeout error when never connected', async () => {
        const { webSocketService, WebSocketConnectionTimeoutError, latestClient } = await loadService()

        const connectPromise = webSocketService.connect(50)
        const timeoutAssertion = expect(connectPromise).rejects.toBeInstanceOf(WebSocketConnectionTimeoutError)

        expect(latestClient().activate).toHaveBeenCalledTimes(1)
        await vi.advanceTimersByTimeAsync(150)
        await timeoutAssertion
    })

    it('queues subscription and re-subscribes on connect callback', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()
        const callback = vi.fn()

        webSocketService.subscribe('/topic/progress', callback, 'sub-1')
        expect(latestClient().subscribe).not.toHaveBeenCalled()

        latestConfig().onConnect()

        expect(latestClient().subscribe).toHaveBeenCalledWith(
            '/topic/progress',
            expect.any(Function),
            { id: 'sub-1' }
        )
    })

    it('handles failed reconnect attempts without unhandled rejection', async () => {
        const { webSocketService, latestConfig } = await loadService()
        const connectSpy = vi.spyOn(webSocketService, 'connect')
            .mockRejectedValue(new Error('reconnect failed'))

        latestConfig().onStompError(new Error('stomp error'))
        // If the service failed to catch the rejection here, Jest would flag an
        // unhandled promise rejection for the worker when timers advance.
        await vi.advanceTimersByTimeAsync(5000)

        expect(connectSpy).toHaveBeenCalled()

        connectSpy.mockRestore()
    })

    it('builds wss URL when document is served over https', async () => {
        Object.defineProperty(document, 'baseURI', {
            configurable: true,
            value: 'https://example.com/app/',
        })
        const { latestConfig } = await loadService()
        expect(latestConfig().brokerURL).toBe('wss://example.com/app/web/ws')
    })

    it('resolves connect immediately when already connected', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()

        latestConfig().onConnect()
        await expect(webSocketService.connect()).resolves.toBeUndefined()
        // activate() must not be re-invoked on an already-connected client
        expect(latestClient().activate).not.toHaveBeenCalled()
    })

    it('resolves connect once onConnect fires within the timeout window', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()

        const connectPromise = webSocketService.connect(500)
        expect(latestClient().activate).toHaveBeenCalledTimes(1)

        // Simulate STOMP connecting mid-poll, then advance past the 100ms poll interval.
        await vi.advanceTimersByTimeAsync(150)
        latestConfig().onConnect()
        await vi.advanceTimersByTimeAsync(150)
        await expect(connectPromise).resolves.toBeUndefined()
    })

    it('rejects connect when the client is not initialized', async () => {
        const { webSocketService } = await loadService()
        // Mirror the guard: null client → immediate rejection.
        ;(webSocketService as unknown as { client: unknown }).client = null

        await expect(webSocketService.connect()).rejects.toThrow('WebSocket client not initialized')
    })

    it('onDisconnect flips connection status to false', async () => {
        const { webSocketService, latestConfig } = await loadService()

        latestConfig().onConnect()
        expect(webSocketService.getConnectionStatus()).toBe(true)

        latestConfig().onDisconnect()
        expect(webSocketService.getConnectionStatus()).toBe(false)
    })

    it('stops scheduling retries after max reconnect attempts', async () => {
        const { webSocketService, latestConfig } = await loadService()
        const connectSpy = vi.spyOn(webSocketService, 'connect')
            .mockRejectedValue(new Error('still failing'))

        // Service caps at 5 reconnect attempts; a 6th onStompError must not schedule one.
        for (let i = 0; i < 5; i++) {
            latestConfig().onStompError(new Error(`attempt ${i + 1}`))
            await vi.advanceTimersByTimeAsync(5000 * (i + 1))
        }
        expect(connectSpy).toHaveBeenCalledTimes(5)

        connectSpy.mockClear()
        latestConfig().onStompError(new Error('one-too-many'))
        await vi.advanceTimersByTimeAsync(60_000)
        expect(connectSpy).not.toHaveBeenCalled()

        connectSpy.mockRestore()
    })

    it('disconnect only deactivates an active client', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()

        // Not connected — deactivate should be skipped.
        webSocketService.disconnect()
        expect(latestClient().deactivate).not.toHaveBeenCalled()

        latestConfig().onConnect()
        webSocketService.disconnect()
        expect(latestClient().deactivate).toHaveBeenCalledTimes(1)
    })

    it('subscribes immediately when connected and forwards STOMP messages via callback', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()
        const callback = vi.fn()

        latestConfig().onConnect()
        const id = webSocketService.subscribe('/topic/events', callback, 'sub-x')

        expect(id).toBe('sub-x')
        expect(latestClient().subscribe).toHaveBeenCalledWith(
            '/topic/events',
            expect.any(Function),
            { id: 'sub-x' }
        )

        // Invoke the wrapped STOMP handler to exercise wsMessage construction.
        const stompHandler = latestClient().subscribe.mock.calls[0][1]
        stompHandler({
            body: 'payload',
            headers: { 'message-id': '1' },
            command: 'MESSAGE',
            destination: '/internal/stomp-dest',
        })

        expect(callback).toHaveBeenCalledWith({
            body: 'payload',
            headers: { 'message-id': '1' },
            command: 'MESSAGE',
            // Destination is pinned to the subscription's, not STOMP's message destination.
            destination: '/topic/events',
        })
    })

    it('forwards STOMP messages delivered after onConnect re-subscription', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()
        const callback = vi.fn()

        webSocketService.subscribe('/topic/progress', callback, 'sub-resub')
        latestConfig().onConnect()

        const stompHandler = latestClient().subscribe.mock.calls[0][1]
        stompHandler({
            body: '{}',
            headers: {},
            command: 'MESSAGE',
            destination: '/from-broker',
        })

        expect(callback).toHaveBeenCalledWith({
            body: '{}',
            headers: {},
            command: 'MESSAGE',
            destination: '/topic/progress',
        })
    })

    it('auto-generates a subscription id when none is supplied', async () => {
        const { webSocketService } = await loadService()
        const id = webSocketService.subscribe('/topic/auto', vi.fn())
        expect(id).toMatch(/^sub_\d+_[a-z0-9]+$/)
        expect(webSocketService.getSubscriptions()).toHaveLength(1)
    })

    it('unsubscribe calls the STOMP subscription and removes it from the map', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()

        latestConfig().onConnect()
        webSocketService.subscribe('/topic/x', vi.fn(), 'sub-unsub')
        expect(webSocketService.getSubscriptions()).toHaveLength(1)

        const stompSub = latestClient().subscribe.mock.results[0].value
        webSocketService.unsubscribe('sub-unsub')
        expect(stompSub.unsubscribe).toHaveBeenCalledTimes(1)
        expect(webSocketService.getSubscriptions()).toHaveLength(0)
    })

    it('unsubscribe is a no-op for unknown ids', async () => {
        const { webSocketService, latestClient } = await loadService()
        webSocketService.unsubscribe('never-registered')
        expect(latestClient().subscribe).not.toHaveBeenCalled()
    })

    it('send publishes via the STOMP client when connected', async () => {
        const { webSocketService, latestConfig, latestClient } = await loadService()

        latestConfig().onConnect()
        webSocketService.send('/topic/out', 'hello', { 'x-foo': 'bar' })

        expect(latestClient().publish).toHaveBeenCalledWith({
            destination: '/topic/out',
            body: 'hello',
            headers: { 'x-foo': 'bar' },
        })
    })

    it('send is a no-op when not connected', async () => {
        const { webSocketService, latestClient } = await loadService()
        webSocketService.send('/topic/out', 'hello')
        expect(latestClient().publish).not.toHaveBeenCalled()
    })

    it('getSubscriptions returns the live list of subscriptions', async () => {
        const { webSocketService } = await loadService()
        webSocketService.subscribe('/topic/a', vi.fn(), 'a')
        webSocketService.subscribe('/topic/b', vi.fn(), 'b')

        const subs = webSocketService.getSubscriptions()
        expect(subs.map((s: { id: string }) => s.id)).toEqual(['a', 'b'])
    })
})
