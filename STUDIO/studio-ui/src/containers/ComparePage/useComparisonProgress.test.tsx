import { act, render } from '@testing-library/react'
import { isFinished, readStatus, useComparisonProgress } from './useComparisonProgress'
import type { ComparisonProgress } from './useComparisonProgress'

// One stable pair of functions, so a re-render never looks like a new subscription.
const unsubscribe = vi.fn()
const subscribe = vi.fn((_destination: string, _onBody: (body: string) => void) => ({ unsubscribe }))

vi.mock('services/stompTopic', () => ({
    subscribeTopic: (destination: string, onBody: (body: string) => void) => subscribe(destination, onBody),
}))

// Whether the socket is up; a topic is listened to only over a connection that is.
let connected = true
vi.mock('hooks/useWebSocket', () => ({
    useWebSocket: () => ({ isConnected: connected }),
}))

const Probe = ({ comparisonId, onProgress }: {
    comparisonId: string | null
    onProgress: (progress: ComparisonProgress) => void
}) => {
    onProgress(useComparisonProgress(comparisonId))
    return null
}

/** Pushes a message onto the topic the hook subscribed to. */
const push = (body: string) => {
    const onBody = subscribe.mock.calls.at(-1)?.[1]
    act(() => onBody?.(body))
}

describe('readStatus', () => {
    it('reads a bare status', () => {
        expect(readStatus('COMPLETED')).toEqual({ status: 'COMPLETED', error: null })
    })

    it('reads a failure with its reason', () => {
        expect(readStatus('{"status":"ERROR","message":"Cannot read the file"}'))
            .toEqual({ status: 'ERROR', error: 'Cannot read the file' })
    })

    it('says nothing about a message it does not understand', () => {
        expect(readStatus('null')).toEqual({ status: null, error: null })
        expect(readStatus('42')).toEqual({ status: null, error: null })
        expect(readStatus('WHATEVER')).toEqual({ status: null, error: null })
        expect(readStatus('{"status":"WHATEVER"}')).toEqual({ status: null, error: null })
        expect(readStatus('')).toEqual({ status: null, error: null })
    })
})

describe('isFinished', () => {
    it('knows the statuses a comparison stops at', () => {
        expect(isFinished('COMPLETED')).toBe(true)
        expect(isFinished('ERROR')).toBe(true)
        expect(isFinished('INTERRUPTED')).toBe(true)
        expect(isFinished('STARTED')).toBe(false)
        expect(isFinished('PENDING')).toBe(false)
        expect(isFinished(null)).toBe(false)
    })
})

describe('useComparisonProgress', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        connected = true
    })

    it('watches nothing until a comparison is named', () => {
        const onProgress = vi.fn()
        render(<Probe comparisonId={null} onProgress={onProgress} />)

        expect(subscribe).not.toHaveBeenCalled()
        expect(onProgress).toHaveBeenLastCalledWith({ status: null, error: null, subscribed: false })
    })

    it('says it is not listening until the connection is up', () => {
        connected = false
        const onProgress = vi.fn()
        const { rerender } = render(<Probe comparisonId="cmp-1" onProgress={onProgress} />)

        expect(subscribe).not.toHaveBeenCalled()
        expect(onProgress).toHaveBeenLastCalledWith({ status: null, error: null, subscribed: false })

        connected = true
        rerender(<Probe comparisonId="cmp-1" onProgress={onProgress} />)

        expect(subscribe).toHaveBeenCalledWith('/user/topic/compare/cmp-1/status', expect.any(Function))
        expect(onProgress).toHaveBeenLastCalledWith({ status: null, error: null, subscribed: true })
    })

    it('reports what the comparison says about itself', () => {
        const onProgress = vi.fn()
        render(<Probe comparisonId="cmp-1" onProgress={onProgress} />)

        expect(subscribe).toHaveBeenCalledWith('/user/topic/compare/cmp-1/status', expect.any(Function))

        push('STARTED')
        expect(onProgress).toHaveBeenLastCalledWith({ status: 'STARTED', error: null, subscribed: true })

        push('{"status":"ERROR","message":"broken"}')
        expect(onProgress).toHaveBeenLastCalledWith({ status: 'ERROR', error: 'broken', subscribed: true })
    })

    it('keeps the last status a message it cannot read arrives after', () => {
        const onProgress = vi.fn()
        render(<Probe comparisonId="cmp-1" onProgress={onProgress} />)

        push('STARTED')
        push('null')

        expect(onProgress).toHaveBeenLastCalledWith({ status: 'STARTED', error: null, subscribed: true })
    })

    it('stops watching a comparison it is done with', () => {
        const { unmount } = render(<Probe comparisonId="cmp-1" onProgress={vi.fn()} />)

        unmount()

        expect(unsubscribe).toHaveBeenCalledTimes(1)
    })

    it('watches the comparison that replaced the previous one', () => {
        const onProgress = vi.fn()
        const { rerender } = render(<Probe comparisonId="cmp-1" onProgress={onProgress} />)
        push('COMPLETED')

        rerender(<Probe comparisonId="cmp-2" onProgress={onProgress} />)

        expect(unsubscribe).toHaveBeenCalledTimes(1)
        expect(subscribe).toHaveBeenLastCalledWith('/user/topic/compare/cmp-2/status', expect.any(Function))
        expect(onProgress).toHaveBeenLastCalledWith({ status: null, error: null, subscribed: true })
    })
})
