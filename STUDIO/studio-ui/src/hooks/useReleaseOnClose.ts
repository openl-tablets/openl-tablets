import { useEffect } from 'react'

/**
 * Lets go of something the screen is holding on the server, once, whenever the screen stops holding it.
 *
 * <p>Two things end a hold and either may come first: the screen going away — another address, another project,
 * the component unmounting — and the page itself closing. The first of them releases and the other finds nothing
 * left to do, so the server is told exactly once.
 *
 * <p>A page frozen for the back/forward cache is not a close: it may be restored with the screen exactly as the
 * reader left it, and what it was holding has to still be there. So a freeze is left alone, and the release
 * waits for a close that means it.
 *
 * <p>`release` is what is being held, so it is kept for as long as the hold lasts: pass a memoized callback, and
 * null while the screen holds nothing. A new callback reads as a new hold, and lets the old one go.
 */
export const useReleaseOnClose = (release: (() => void) | null): void => {
    useEffect(() => {
        if (release === null) {
            return undefined
        }
        let done = false
        const letGo = () => {
            if (done) {
                return
            }
            done = true
            release()
        }
        const onHide = (event: PageTransitionEvent) => {
            if (!event.persisted) {
                letGo()
            }
        }
        window.addEventListener('pagehide', onHide)
        return () => {
            window.removeEventListener('pagehide', onHide)
            letGo()
        }
    }, [release])
}

export default useReleaseOnClose
