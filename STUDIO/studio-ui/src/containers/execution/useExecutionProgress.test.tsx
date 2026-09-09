import { act, renderHook } from '@testing-library/react'
import { isFinished, useExecutionProgress } from 'containers/execution/useExecutionProgress'

// The socket is the session's; here the screen is handed the frames itself. The subscriber keeps its identity
// across renders, the way the real hook does: a new one on every render would resubscribe without end.
const { listeners, subscribe, unsubscribe } = vi.hoisted(() => {
    const listeners = { current: [] as ((message: { body: string }) => void)[] }
    return {
        listeners,
        subscribe: (_topic: string, onMessage: (message: { body: string }) => void, id: string) => {
            listeners.current.push(onMessage)
            return id
        },
        unsubscribe: (_id: string) => undefined,
    }
})

vi.mock('hooks/useWebSocket', () => ({
    useWebSocket: () => ({ isConnected: true, subscribe, unsubscribe }),
}))

const report = (body: string) => act(() => listeners.current.forEach(listener => listener({ body })))

describe('useExecutionProgress', () => {
    beforeEach(() => {
        listeners.current = []
    })

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

    it('forgets what the last run reported when it is asked to', () => {
        const { result } = renderHook(() => useExecutionProgress('/topic/run/status'))

        report(JSON.stringify({ status: 'ERROR', message: 'Division by zero' }))
        act(() => result.current.reset())

        expect(result.current.status).toBeNull()
        expect(result.current.error).toBeNull()
    })
})
