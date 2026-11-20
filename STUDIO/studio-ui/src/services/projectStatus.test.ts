import {
    fresherStatus,
    isReadBefore,
    isSameStatus,
    type ProjectStatusUpdate,
    type subscribeProjectStatus as SubscribeProjectStatusFn,
    type subscribeWorkspaceProjectStatuses as SubscribeWorkspaceProjectStatusesFn,
} from 'services/projectStatus'

vi.mock('services/websocket', () => ({
    webSocketService: {
        connect: vi.fn().mockResolvedValue(undefined),
        subscribe: vi.fn(),
        unsubscribe: vi.fn(),
    },
}))

describe('projectStatus service', () => {
    let subscribeProjectStatus: typeof SubscribeProjectStatusFn
    let subscribeWorkspaceProjectStatuses: typeof SubscribeWorkspaceProjectStatusesFn
    let mockedWebSocketService: {
        connect: ReturnType<typeof vi.fn>
        subscribe: ReturnType<typeof vi.fn>
        unsubscribe: ReturnType<typeof vi.fn>
    }

    beforeEach(async () => {
        vi.useFakeTimers()
        vi.clearAllMocks()
        vi.resetModules()
        const wsModule = await import('services/websocket')
        mockedWebSocketService = wsModule.webSocketService as unknown as typeof mockedWebSocketService
        mockedWebSocketService.subscribe.mockReturnValue('sub-1')
        const mod = await import('services/projectStatus')
        subscribeProjectStatus = mod.subscribeProjectStatus
        subscribeWorkspaceProjectStatuses = mod.subscribeWorkspaceProjectStatuses
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    describe('subscribeProjectStatus', () => {
        it('subscribes to branchless topic when branch is null', () => {
            subscribeProjectStatus('abc=', null, () => undefined)

            expect(mockedWebSocketService.connect).toHaveBeenCalled()
            expect(mockedWebSocketService.subscribe).toHaveBeenCalledWith(
                '/user/topic/projects/abc%3D/status',
                expect.any(Function),
                undefined,
                expect.any(Function)
            )
        })

        it('subscribes to branch-scoped topic when branch is provided', () => {
            subscribeProjectStatus('abc=', 'feature/x', () => undefined)

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledWith(
                '/user/topic/projects/abc%3D/branches/feature%2Fx/status',
                expect.any(Function),
                undefined,
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

        it('tells every listener when the subscription starts, again after a reconnect', () => {
            const listenerA = vi.fn()
            const listenerB = vi.fn()
            subscribeProjectStatus('abc=', null, vi.fn(), listenerA)
            subscribeProjectStatus('abc=', null, vi.fn(), listenerB)
            const started = mockedWebSocketService.subscribe.mock.calls[0]![3] as (sequence: number) => void

            started(20)
            started(40)

            expect(listenerA.mock.calls).toEqual([[20], [40]])
            expect(listenerB.mock.calls).toEqual([[20], [40]])
        })

        it('tells a listener joining a subscription that already started its number at once', async () => {
            subscribeProjectStatus('abc=', null, vi.fn(), vi.fn())
            const started = mockedWebSocketService.subscribe.mock.calls[0]![3] as (sequence: number) => void
            started(20)
            const late = vi.fn()

            subscribeProjectStatus('abc=', null, vi.fn(), late)
            expect(late).not.toHaveBeenCalled()
            await Promise.resolve()

            expect(late).toHaveBeenCalledWith(20)
        })

        it('tells a listener that left before its turn nothing', async () => {
            subscribeProjectStatus('abc=', null, vi.fn(), vi.fn())
            const started = mockedWebSocketService.subscribe.mock.calls[0]![3] as (sequence: number) => void
            started(20)
            const late = vi.fn()

            subscribeProjectStatus('abc=', null, vi.fn(), late).unsubscribe()
            await Promise.resolve()

            expect(late).not.toHaveBeenCalled()
        })

        it('re-subscribes after the cooldown completes', () => {
            subscribeProjectStatus('abc=', null, vi.fn()).unsubscribe()
            vi.advanceTimersByTime(5000)
            subscribeProjectStatus('abc=', null, vi.fn())

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledTimes(2)
        })
    })

    describe('subscribeWorkspaceProjectStatuses', () => {
        it('holds one subscription for the statuses of every project', () => {
            const onUpdate = vi.fn()
            subscribeWorkspaceProjectStatuses(onUpdate)

            expect(mockedWebSocketService.subscribe).toHaveBeenCalledWith(
                '/user/topic/workspace/projects/status',
                expect.any(Function),
                undefined,
                expect.any(Function)
            )
            // Each update names its own project — the screen routes it, not the transport.
            const stompCallback = mockedWebSocketService.subscribe.mock.calls[0]![1] as (msg: { body: string }) => void
            stompCallback({ body: JSON.stringify({ projectId: 'abc=', compileState: 'errors' }) })
            expect(onUpdate).toHaveBeenCalledWith({ projectId: 'abc=', compileState: 'errors' })
        })
    })
})

describe('project status freshness', () => {
    const status = (sequence: number, compileState: ProjectStatusUpdate['compileState'] = 'ok'): ProjectStatusUpdate =>
        ({ projectId: 'p1', compileState, sequence })

    it('keeps the status the server read later, whichever arrives last', () => {
        const read = status(10)
        const pushed = status(20, 'compiling')

        expect(fresherStatus(read, pushed)).toBe(pushed)
        expect(fresherStatus(pushed, read)).toBe(pushed)
    })

    it('takes any status over none', () => {
        const read = status(10)

        expect(fresherStatus(null, read)).toBe(read)
        expect(fresherStatus(read, null)).toBe(read)
        expect(fresherStatus(null, null)).toBeNull()
    })

    it('tells a status read before the subscription started', () => {
        expect(isReadBefore(status(10), 20)).toBe(true)
        expect(isReadBefore(status(30), 20)).toBe(false)
    })

    it('tells two statuses saying the same apart from the moment they were read', () => {
        expect(isSameStatus(status(10), status(20))).toBe(true)
        expect(isSameStatus(status(10), status(20, 'errors'))).toBe(false)
        expect(isSameStatus(null, null)).toBe(true)
        expect(isSameStatus(status(10), null)).toBe(false)
    })
})
