import { act, renderHook } from '@testing-library/react'
import { isFinished, useExecutionProgress } from 'containers/execution/useExecutionProgress'

// The socket is the session's; here it is a fake one the test pushes frames into. The subscriptions go
// through the real multiplexer, so what a second screen watching the same topic does to the first is what it
// would do in the browser.
const { socket, webSocketService } = vi.hoisted(() => {
    interface Subscribed { destination: string, callback: (message: { body: string }) => void }
    const socket = { current: new Map<string, Subscribed>() }
    let taken = 0
    return {
        socket,
        webSocketService: {
            connect: () => Promise.resolve(),
            // Named the way the real service names a subscription: a caller may ask for a name, and asking
            // for one already taken replaces the subscription that holds it.
            subscribe: (
                destination: string,
                callback: (message: { body: string }) => void,
                subscriptionId?: string
            ) => {
                const id = subscriptionId || `sub-${(taken += 1)}`
                socket.current.set(id, { destination, callback })
                return id
            },
            unsubscribe: (id: string) => socket.current.delete(id),
        },
    }
})

vi.mock('hooks/useWebSocket', () => ({
    useWebSocket: () => ({ isConnected: true }),
}))

vi.mock('services/websocket', () => ({ webSocketService }))

// The server pushes one frame per STOMP subscription; the multiplexer hands it to every screen listening.
const reportOn = (topic: string, body: string) => act(() => socket.current.forEach(subscribed => {
    if (subscribed.destination === topic) {
        subscribed.callback({ body })
    }
}))

const report = (body: string) => reportOn('/topic/run/status', body)

describe('useExecutionProgress', () => {
    // The multiplexer keeps a topic for a short while after its last listener leaves, so the fake socket is
    // left as it is between tests: the next subscriber of a topic finds it and is handed its frames.

    it('follows the status a run reports, quoted as JSON or bare', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        expect(result.current.subscribed).toBe(true)

        report('"STARTED"')
        expect(result.current.status).toBe('STARTED')

        report('COMPLETED')
        expect(result.current.status).toBe('COMPLETED')
        expect(isFinished(result.current.status)).toBe(true)
    })

    it('reads the reason out of a status that carries one', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        report(JSON.stringify({ status: 'ERROR', message: 'Division by zero' }))

        expect(result.current.status).toBe('ERROR')
        expect(result.current.error).toBe('Division by zero')
    })

    it('keeps what it knows when a frame says nothing it understands', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        report('"STARTED"')
        // A frame of `null`, of a number, or of a status this screen does not know, leaves the status alone.
        report('null')
        report('42')
        report('"WHAT_IS_THIS"')
        report(JSON.stringify({ status: null }))

        expect(result.current.status).toBe('STARTED')
        expect(result.current.error).toBeNull()
    })

    it('keeps the reason a run failed for whatever is reported after it', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        report(JSON.stringify({ status: 'ERROR', message: 'Division by zero' }))
        // A frame carrying no reason — a bare status, or one this screen does not know — says nothing about
        // why the run failed, so the window keeps showing it.
        report('"ERROR"')
        report(JSON.stringify({ status: 'ERROR' }))
        report('null')

        expect(result.current.error).toBe('Division by zero')
    })

    it('lets two screens follow the same execution', () => {
        // The window showing a result and the panel that started the run watch one topic. A subscription
        // named after the topic would be the same name for both, and the second would replace the first.
        const panel = renderHook(() => useExecutionProgress('/topic/run/status'))
        const window = renderHook(() => useExecutionProgress('/topic/run/status'))

        report(JSON.stringify({ status: 'COMPLETED' }))

        expect(panel.result.current.status).toBe('COMPLETED')
        expect(window.result.current.status).toBe('COMPLETED')
    })

    it('counts the results of a run that reports them, on their own topic', () => {
        const { result } = renderHook(
            () => useExecutionProgress('/topic/tests/status', '/topic/tests/results')
        )

        reportOn('/topic/tests/results', '{}')
        reportOn('/topic/tests/results', '{}')

        expect(result.current.arrived).toBe(2)
    })

    it('forgets what the last run reported when it is asked to', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        report(JSON.stringify({ status: 'ERROR', message: 'Division by zero' }))
        act(() => result.current.reset())

        expect(result.current.status).toBeNull()
        expect(result.current.error).toBeNull()
    })
})
