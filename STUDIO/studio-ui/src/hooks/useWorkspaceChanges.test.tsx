import { render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from '../services/apiCall'
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

    it('holds a ping close on the heels of the user\'s own change and delivers it after the echo window', () => {
        // Pinned to a distant date so the echo mark cannot leak into the other tests' clocks.
        vi.setSystemTime(new Date('2000-01-01T00:00:00Z'))
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        // The user's own mutation announced itself; the ping right after it is likely its echo —
        // but a real change of someone else can hide behind it, so it must not be dropped.
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        ping()
        vi.advanceTimersByTime(500)
        expect(onChange).not.toHaveBeenCalled()

        // Once the echo window passes, the held batch arrives as one quiet refresh.
        vi.advanceTimersByTime(2100)
        expect(onChange).toHaveBeenCalledTimes(1)

        // A ping later on is someone else's change and goes through directly.
        vi.setSystemTime(new Date('2000-01-01T00:01:00Z'))
        ping()
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(2)
    })

    it('delivers a held batch even while the user keeps acting, once the hold cap passes', () => {
        // A user mutating more often than the echo window must not starve the delivery forever —
        // that would suppress exactly the other-user changes the hold exists to preserve.
        vi.setSystemTime(new Date('2000-02-01T00:00:00Z'))
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        ping()
        // Every 2 s another own mutation lands, keeping the echo window permanently open.
        for (let i = 0; i < 6; i++) {
            vi.advanceTimersByTime(2000)
            window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        }
        vi.advanceTimersByTime(2600)

        expect(onChange).toHaveBeenCalledTimes(1)
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
