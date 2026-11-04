import { type RefObject, useLayoutEffect, useState } from 'react'

/** The size of the window: what a list is taken to be shown in before the element it scrolls in is measured. */
export const windowRect = (): { width: number, height: number } => ({ width: window.innerWidth, height: window.innerHeight })

/**
 * Where a list drawn only while in sight begins in what scrolls it: how far its first item lies below the top of the
 * element it scrolls in, or of the page when the window scrolls it. The list is told so to know which of its items are
 * in sight.
 *
 * Measured again whenever what is scrolled changes its size: what stands above the list — headings, alerts, options
 * wrapped onto more lines — moves where it begins, and what is scrolled grows or shrinks with it.
 *
 * @param anchor   the element the list begins at
 * @param scroller the element the list scrolls in, null while it is not drawn yet; nothing for the window
 * @param enabled  whether the list is drawn as it scrolls, and so measured at all
 */
export const useScrollMargin = (
    anchor: RefObject<HTMLElement | null>,
    scroller: HTMLElement | null | undefined,
    enabled: boolean
): number => {
    const [margin, setMargin] = useState(0)
    useLayoutEffect(() => {
        const scrolled = scroller === undefined ? document.body : scroller?.firstElementChild
        if (!enabled || scroller === null || scrolled == null) {
            return undefined
        }
        const measure = () => {
            const top = anchor.current?.getBoundingClientRect().top
            if (top === undefined) {
                return
            }
            const at = scroller === undefined
                ? top + window.scrollY
                : top - scroller.getBoundingClientRect().top + scroller.scrollTop
            setMargin(current => (Math.abs(at - current) > 0.5 ? at : current))
        }
        measure()
        const observer = new ResizeObserver(measure)
        observer.observe(scrolled)
        return () => observer.disconnect()
    }, [anchor, enabled, scroller])
    return margin
}
