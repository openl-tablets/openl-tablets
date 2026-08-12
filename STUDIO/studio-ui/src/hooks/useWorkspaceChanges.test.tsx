import { render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from '../services/apiCall'
import type { ChangePing } from '../services/changePing'
import { CLIENT_ID } from '../services/clientId'
import { subscribeWorkspaceChanges } from '../services/workspaceChanges'
import { useWorkspaceChanges } from './useWorkspaceChanges'

vi.mock('../services/workspaceChanges', () => ({ subscribeWorkspaceChanges: vi.fn() }))

const Probe = ({ onChange, holdWhile = false }: { onChange: () => void, holdWhile?: boolean }) => {
    useWorkspaceChanges(onChange, { holdWhile })
    return null
}

/** A change made outside a request — the files watcher, a repository poll — attributed to nobody. */
const UNATTRIBUTED: ChangePing = { files: [], origins: []}

describe('useWorkspaceChanges', () => {
    let ping: (ping: ChangePing) => void
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

        ping(UNATTRIBUTED)
        ping(UNATTRIBUTED)
        ping(UNATTRIBUTED)
        expect(onChange).not.toHaveBeenCalled()

        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(1)

        // A ping after the window opens a new refresh.
        ping(UNATTRIBUTED)
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(2)
    })

    it('always calls the latest callback, so an inline closure never goes stale', () => {
        const first = vi.fn()
        const second = vi.fn()
        const { rerender } = render(<Probe onChange={first} />)
        rerender(<Probe onChange={second} />)

        ping(UNATTRIBUTED)
        vi.advanceTimersByTime(500)

        expect(first).not.toHaveBeenCalled()
        expect(second).toHaveBeenCalledTimes(1)
        // The subscription itself survives rerenders — one for the component's whole life.
        expect(subscribeWorkspaceChanges).toHaveBeenCalledTimes(1)
    })

    it('drops a ping this tab caused itself, and keeps one that stands for another session too', () => {
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        // The screen reloaded itself when the action finished — re-reading would only repeat it.
        ping({ files: [], origins: [CLIENT_ID]})
        vi.advanceTimersByTime(3000)
        expect(onChange).not.toHaveBeenCalled()

        // The debounce window coalesced another session's change into the same ping: not an echo.
        ping({ files: [], origins: [CLIENT_ID, 'another-tab']})
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(1)
    })

    it('lets a change of another session through at once, whatever the user is doing', () => {
        vi.setSystemTime(new Date('2000-03-01T00:00:00Z'))
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        // The user has just acted here, but the ping names its origin and it is not this tab —
        // there is nothing to wait out.
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        ping({ files: [], origins: ['another-tab']})
        vi.advanceTimersByTime(500)

        expect(onChange).toHaveBeenCalledTimes(1)
    })

    it('holds an unattributed ping close on the heels of the user\'s own change', () => {
        // Pinned to a distant date so the echo mark cannot leak into the other tests' clocks.
        vi.setSystemTime(new Date('2000-01-01T00:00:00Z'))
        const onChange = vi.fn()
        render(<Probe onChange={onChange} />)

        // A change made outside a request names no origin, so it may still be this tab's own action
        // reaching the workspace disk — but a real change of someone else can hide behind it too.
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        ping(UNATTRIBUTED)
        vi.advanceTimersByTime(500)
        expect(onChange).not.toHaveBeenCalled()

        // Once the echo window passes, the held batch arrives as one quiet refresh.
        vi.advanceTimersByTime(2100)
        expect(onChange).toHaveBeenCalledTimes(1)

        // A ping later on cannot be an echo of anything and goes through directly.
        vi.setSystemTime(new Date('2000-01-01T00:01:00Z'))
        ping(UNATTRIBUTED)
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
        ping(UNATTRIBUTED)
        // Every 2 s another own mutation lands, keeping the echo window permanently open.
        for (let i = 0; i < 6; i++) {
            vi.advanceTimersByTime(2000)
            window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        }
        vi.advanceTimersByTime(2600)

        expect(onChange).toHaveBeenCalledTimes(1)
    })

    it('waits for an action of the screen\'s own before refreshing, and never drops the ping', () => {
        const onChange = vi.fn()
        const { rerender } = render(<Probe holdWhile onChange={onChange} />)

        // The user pressed a button; its own read is on the wire, and a refresh started beside it
        // would supersede the answer that read brings back.
        ping({ files: [], origins: ['another-tab']})
        vi.advanceTimersByTime(3000)
        expect(onChange).not.toHaveBeenCalled()

        // The action finished and its answer is on screen, so the change of the other session lands.
        rerender(<Probe holdWhile={false} onChange={onChange} />)
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledTimes(1)
    })

    it('unsubscribes and drops a pending refresh on unmount', () => {
        const onChange = vi.fn()
        const { unmount } = render(<Probe onChange={onChange} />)

        ping(UNATTRIBUTED)
        unmount()
        vi.advanceTimersByTime(500)

        expect(unsubscribe).toHaveBeenCalled()
        expect(onChange).not.toHaveBeenCalled()
    })
})
