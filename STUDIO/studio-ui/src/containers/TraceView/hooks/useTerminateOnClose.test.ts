import { renderHook } from '@testing-library/react'
import traceService from 'services/traceService'
import { stampTraceLaunch } from 'services/traceLaunchToken'
import useTerminateOnClose from './useTerminateOnClose'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        releaseOnClose: vi.fn(),
    },
}))

const releaseOnClose = traceService.releaseOnClose as ReturnType<typeof vi.fn>

/** Dispatch a pagehide event, optionally persisted (a back/forward-cache freeze). */
const firePagehide = (persisted = false): void => {
    const event = new Event('pagehide')
    Object.defineProperty(event, 'persisted', { value: persisted })
    window.dispatchEvent(event)
}

describe('useTerminateOnClose', () => {
    // jsdom ships no Web Storage, so the launch-token guard gets an in-memory one for this file.
    const memory = new Map<string, string>()

    beforeEach(() => {
        vi.clearAllMocks()
        memory.clear()
        vi.stubGlobal('localStorage', {
            getItem: (key: string) => memory.get(key) ?? null,
            setItem: (key: string, value: string) => memory.set(key, value),
            removeItem: (key: string) => memory.delete(key),
            clear: () => memory.clear(),
        })
    })

    it('terminates the session when the window closes', () => {
        // The launcher always stamps a token before opening the debugger window.
        stampTraceLaunch()
        renderHook(() => useTerminateOnClose('p1'))

        firePagehide()

        expect(releaseOnClose).toHaveBeenCalledWith('p1')
    })

    it('keeps the session when the page is only frozen for the back/forward cache', () => {
        stampTraceLaunch()
        renderHook(() => useTerminateOnClose('p1'))

        firePagehide(true)

        expect(releaseOnClose).not.toHaveBeenCalled()
    })

    it('keeps the session when the launch token cannot be read (blocked storage)', () => {
        // Private mode / disabled storage: every read fails, so ownership can never be verified.
        vi.stubGlobal('localStorage', {
            getItem: () => { throw new Error('storage disabled') },
            setItem: () => { throw new Error('storage disabled') },
        })
        renderHook(() => useTerminateOnClose('p1'))

        firePagehide()

        expect(releaseOnClose).not.toHaveBeenCalled()
    })

    it('keeps the session when the launcher has reused this window for a newer trace', () => {
        stampTraceLaunch()
        renderHook(() => useTerminateOnClose('p1'))

        // A newer launch stamps a fresh token, so this outgoing document no longer owns the session.
        stampTraceLaunch()
        firePagehide()

        expect(releaseOnClose).not.toHaveBeenCalled()
    })

    it('stops listening once unmounted', () => {
        stampTraceLaunch()
        const { unmount } = renderHook(() => useTerminateOnClose('p1'))
        unmount()

        firePagehide()

        expect(releaseOnClose).not.toHaveBeenCalled()
    })

    it('does nothing without a project', () => {
        renderHook(() => useTerminateOnClose(undefined))

        firePagehide()

        expect(releaseOnClose).not.toHaveBeenCalled()
    })
})
