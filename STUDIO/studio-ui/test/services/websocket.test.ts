describe('webSocketService', () => {
    const hadOwnBaseUri = Object.prototype.hasOwnProperty.call(document, 'baseURI')
    const originalBaseUriDescriptor = Object.getOwnPropertyDescriptor(document, 'baseURI')
    const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {})

    beforeEach(() => {
        jest.resetModules()
        jest.useFakeTimers()
        jest.clearAllMocks()
        Object.defineProperty(document, 'baseURI', {
            configurable: true,
            value: 'http://localhost:8080/webstudio/',
        })
    })

    afterEach(() => {
        jest.useRealTimers()
        if (hadOwnBaseUri && originalBaseUriDescriptor) {
            Object.defineProperty(document, 'baseURI', originalBaseUriDescriptor)
        } else {
            // Remove test override and fall back to prototype-provided baseURI.
            delete (document as { baseURI?: string }).baseURI
        }
    })

    afterAll(() => {
        consoleWarnSpy.mockRestore()
    })

    const loadService = () => {
        let latestConfig: any
        let latestClient: any

        jest.doMock('@stomp/stompjs', () => {
            class MockClient {
                config: any
                activate = jest.fn()
                deactivate = jest.fn()
                subscribe = jest.fn(() => ({ unsubscribe: jest.fn() }))
                publish = jest.fn()

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

        let webSocketService: any
        let WebSocketConnectionTimeoutError: any
        jest.isolateModules(() => {
            const mod = require('services/websocket')
            webSocketService = mod.webSocketService
            WebSocketConnectionTimeoutError = mod.WebSocketConnectionTimeoutError
        })

        return {
            webSocketService,
            WebSocketConnectionTimeoutError,
            latestConfig: () => latestConfig,
            latestClient: () => latestClient,
        }
    }

    it('builds websocket URL from base URI', () => {
        const { latestConfig } = loadService()

        expect(latestConfig().brokerURL).toBe('ws://localhost:8080/webstudio/web/ws')
    })

    it('rejects connect with timeout error when never connected', async () => {
        const { webSocketService, WebSocketConnectionTimeoutError, latestClient } = loadService()

        const connectPromise = webSocketService.connect(50)
        const timeoutAssertion = expect(connectPromise).rejects.toBeInstanceOf(WebSocketConnectionTimeoutError)

        expect(latestClient().activate).toHaveBeenCalledTimes(1)
        await jest.advanceTimersByTimeAsync(150)
        await timeoutAssertion
    })

    it('queues subscription and re-subscribes on connect callback', () => {
        const { webSocketService, latestConfig, latestClient } = loadService()
        const callback = jest.fn()

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
        const { webSocketService, latestConfig } = loadService()
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})
        const connectSpy = jest
            .spyOn(webSocketService, 'connect')
            .mockRejectedValue(new Error('reconnect failed'))

        latestConfig().onStompError(new Error('stomp error'))
        await jest.advanceTimersByTimeAsync(5000)

        expect(connectSpy).toHaveBeenCalled()
        expect(consoleErrorSpy).toHaveBeenCalledWith(
            'WebSocket reconnect attempt failed:',
            expect.any(Error)
        )

        connectSpy.mockRestore()
        consoleErrorSpy.mockRestore()
    })
})
