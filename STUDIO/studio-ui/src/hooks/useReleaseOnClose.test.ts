import { renderHook } from '@testing-library/react'
import { useReleaseOnClose } from './useReleaseOnClose'

/** Dispatch a pagehide event, optionally persisted (a back/forward-cache freeze). */
const firePagehide = (persisted = false): void => {
    const event = new Event('pagehide')
    Object.defineProperty(event, 'persisted', { value: persisted })
    window.dispatchEvent(event)
}

describe('useReleaseOnClose', () => {
    it('lets go when the screen stops holding', () => {
        const release = vi.fn()
        const { rerender } = renderHook(({ held }: { held: boolean }) =>
            useReleaseOnClose(held ? release : null), { initialProps: { held: true } })

        expect(release).not.toHaveBeenCalled()
        rerender({ held: false })

        expect(release).toHaveBeenCalledTimes(1)
    })

    it('lets go when the component goes away', () => {
        const release = vi.fn()
        const { unmount } = renderHook(() => useReleaseOnClose(release))

        unmount()

        expect(release).toHaveBeenCalledTimes(1)
    })

    it('lets go when the page closes, and only once however the hold ends', () => {
        const release = vi.fn()
        const { unmount } = renderHook(() => useReleaseOnClose(release))

        firePagehide()
        unmount()

        // The first of the two releases and the other finds nothing left to do.
        expect(release).toHaveBeenCalledTimes(1)
    })

    it('holds on through a back/forward-cache freeze, which may be restored', () => {
        const release = vi.fn()
        renderHook(() => useReleaseOnClose(release))

        firePagehide(true)

        // The screen may come back exactly as the reader left it, and what it holds has to still be there.
        expect(release).not.toHaveBeenCalled()
    })

    it('holds nothing while it is given nothing to let go of', () => {
        const release = vi.fn()
        const { unmount } = renderHook(() => useReleaseOnClose(null))

        firePagehide()
        unmount()

        expect(release).not.toHaveBeenCalled()
    })
})
