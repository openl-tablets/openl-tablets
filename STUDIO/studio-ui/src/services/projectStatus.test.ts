import type { fetchProjectStatus as FetchProjectStatusFn, subscribeProjectStatus as SubscribeProjectStatusFn } from 'services/projectStatus'

vi.mock('services/config', () => ({
    __esModule: true,
    default: { CONTEXT: '/ctx' },
}))

vi.mock('services/websocket', () => ({
    webSocketService: {
        connect: vi.fn().mockResolvedValue(undefined),
        subscribe: vi.fn(),
        unsubscribe: vi.fn(),
    },
}))

describe('projectStatus service', () => {
    const fetchMock = vi.fn()
    let fetchProjectStatus: typeof FetchProjectStatusFn
    let subscribeProjectStatus: typeof SubscribeProjectStatusFn
    let mockedWebSocketService: {
        connect: ReturnType<typeof vi.fn>
        subscribe: ReturnType<typeof vi.fn>
        unsubscribe: ReturnType<typeof vi.fn>
    }

    beforeEach(async () => {
        vi.useFakeTimers()
        vi.clearAllMocks()
        vi.resetModules()
        ;(global as any).fetch = fetchMock
        const wsModule = await import('services/websocket')
        mockedWebSocketService = wsModule.webSocketService as unknown as typeof mockedWebSocketService
        mockedWebSocketService.subscribe.mockReturnValue('sub-1')
        const mod = await import('services/projectStatus')
        fetchProjectStatus = mod.fetchProjectStatus
        subscribeProjectStatus = mod.subscribeProjectStatus
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    describe('fetchProjectStatus', () => {
        it('GETs /web/projects/{projectId}/status?branch=', async () => {
            const payload = { projectId: 'abc', compileState: 'ok' }
            fetchMock.mockResolvedValue({
                ok: true,
                status: 200,
                json: async () => payload,
            })

            const result = await fetchProjectStatus('abc=')

            expect(fetchMock).toHaveBeenCalledWith(
                '/ctx/web/projects/abc%3D/status?branch=',
                expect.objectContaining({ credentials: 'same-origin' })
            )
            expect(result).toEqual(payload)
        })

        it('throws on non-2xx response', async () => {
            fetchMock.mockResolvedValue({ ok: false, status: 500, json: async () => ({}) })

            await expect(fetchProjectStatus('abc')).rejects.toThrow('Failed to fetch project status: 500')
        })

        it('dedupes concurrent fetches for the same projectId', async () => {
            const payload = { projectId: 'abc', compileState: 'ok' }
            fetchMock.mockResolvedValue({
                ok: true,
                status: 200,
                json: async () => payload,
            })

            const [first, second] = await Promise.all([
                fetchProjectStatus('abc'),
                fetchProjectStatus('abc'),
            ])

            expect(fetchMock).toHaveBeenCalledTimes(1)
            expect(first).toEqual(payload)
            expect(second).toEqual(payload)
        })

        it('issues a fresh request once the in-flight one settles', async () => {
            fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({ projectId: 'abc' }) })

            await fetchProjectStatus('abc')
            await fetchProjectStatus('abc')

            expect(fetchMock).toHaveBeenCalledTimes(2)
        })
    })

    describe('subscribeProjectStatus', () => {
        it('subscribes to branchless topic when branch is null', () => {
            subscribeProjectStatus('abc=', null, () => undefined)

            expect(mockedWebSocketService.connect).toHaveBeenCalled()
            expect(mockedWebSocketService.subscribe).toHaveBeenCalledWith(
                '/user/topic/projects/abc%3D/status',
                expect.any(Function)
            )
        })

        it('subscribes to branch-scoped topic when branch is provided', () => {
            subscribeProjectStatus('abc=', 'feature/x', () => undefined)

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledWith(
                '/user/topic/projects/abc%3D/branches/feature%2Fx/status',
                expect.any(Function)
            )
        })

        it('parses incoming STOMP message and forwards to callback', () => {
            const onUpdate = vi.fn()
            subscribeProjectStatus('abc=', null, onUpdate)

            const stompCallback = mockedWebSocketService.subscribe.mock.calls[0]![1] as (msg: { body: string }) => void
            stompCallback({ body: JSON.stringify({ projectId: 'abc=', compileState: 'compiling' }) })

            expect(onUpdate).toHaveBeenCalledWith({ projectId: 'abc=', compileState: 'compiling' })
        })

        it('swallows malformed JSON payloads', () => {
            const onUpdate = vi.fn()
            subscribeProjectStatus('abc=', null, onUpdate)

            const stompCallback = mockedWebSocketService.subscribe.mock.calls[0]![1] as (msg: { body: string }) => void
            expect(() => stompCallback({ body: 'not-json' })).not.toThrow()
            expect(onUpdate).not.toHaveBeenCalled()
        })

        it('multiplexes multiple listeners on the same destination', () => {
            const listenerA = vi.fn()
            const listenerB = vi.fn()
            subscribeProjectStatus('abc=', null, listenerA)
            subscribeProjectStatus('abc=', null, listenerB)

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledTimes(1)

            const stompCallback = mockedWebSocketService.subscribe.mock.calls[0]![1] as (msg: { body: string }) => void
            stompCallback({ body: JSON.stringify({ projectId: 'abc=', compileState: 'ok' }) })

            expect(listenerA).toHaveBeenCalledWith({ projectId: 'abc=', compileState: 'ok' })
            expect(listenerB).toHaveBeenCalledWith({ projectId: 'abc=', compileState: 'ok' })
        })

        it('only tears down STOMP subscription when the last listener leaves and cooldown elapses', () => {
            const subA = subscribeProjectStatus('abc=', null, vi.fn())
            const subB = subscribeProjectStatus('abc=', null, vi.fn())

            subA.unsubscribe()
            vi.advanceTimersByTime(10000)
            expect(mockedWebSocketService.unsubscribe).not.toHaveBeenCalled()

            subB.unsubscribe()
            expect(mockedWebSocketService.unsubscribe).not.toHaveBeenCalled()
            vi.advanceTimersByTime(5000)
            expect(mockedWebSocketService.unsubscribe).toHaveBeenCalledWith('sub-1')
        })

        it('rescues the STOMP subscription if a new listener arrives during cooldown', () => {
            const first = subscribeProjectStatus('abc=', null, vi.fn())
            first.unsubscribe()
            vi.advanceTimersByTime(1000)

            subscribeProjectStatus('abc=', null, vi.fn())
            vi.advanceTimersByTime(10000)

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledTimes(1)
            expect(mockedWebSocketService.unsubscribe).not.toHaveBeenCalled()
        })

        it('unsubscribe is idempotent', () => {
            const subscription = subscribeProjectStatus('abc=', null, vi.fn())

            subscription.unsubscribe()
            subscription.unsubscribe()
            vi.advanceTimersByTime(5000)

            expect(mockedWebSocketService.unsubscribe).toHaveBeenCalledTimes(1)
        })

        it('re-subscribes after the cooldown completes', () => {
            subscribeProjectStatus('abc=', null, vi.fn()).unsubscribe()
            vi.advanceTimersByTime(5000)
            subscribeProjectStatus('abc=', null, vi.fn())

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledTimes(2)
        })
    })
})
