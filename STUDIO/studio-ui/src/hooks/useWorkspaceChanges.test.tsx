import { render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeWorkspaceChanges } from '../services/workspaceChanges'
import { useWorkspaceChanges } from './useWorkspaceChanges'

vi.mock('../services/workspaceChanges', () => ({ subscribeWorkspaceChanges: vi.fn() }))

const Probe = ({ onChange }: { onChange: () => void }) => {
    useWorkspaceChanges(onChange)
    return null
}

describe('useWorkspaceChanges', () => {
    let ping: () => void
    const unsubscribe = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        vi.useFakeTimers()
        vi.mocked(subscribeWorkspaceChanges).mockImplementation(onChange => {
            ping = onChange
            return { unsubscribe }
        })
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('collapses a burst of pings into one refresh', () => {
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        ping()
        ping()
        ping()
        expect(onChange).not.toHaveBeenCalled()

        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(1)

        // A ping after the window opens a new refresh.
        ping()
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(2)
    })

    it('always calls the latest callback, so an inline closure never goes stale', () => {
        const first = vi.fn()
        const second = vi.fn()
        const { rerender } = render(<Probe onChange={first} />)
        rerender(<Probe onChange={second} />)

        ping()
        vi.advanceTimersByTime(500)

        expect(first).not.toHaveBeenCalled()
        expect(second).toHaveBeenCalledTimes(1)
        // The subscription itself survives rerenders — one for the component's whole life.
        expect(subscribeWorkspaceChanges).toHaveBeenCalledTimes(1)
    })

    it('unsubscribes and drops a pending refresh on unmount', () => {
        const onChange = vi.fn()
        const { unmount } = render(<Probe onChange={onChange} />)

        ping()
        unmount()
        vi.advanceTimersByTime(500)

        expect(unsubscribe).toHaveBeenCalled()
        expect(onChange).not.toHaveBeenCalled()
    })
})
