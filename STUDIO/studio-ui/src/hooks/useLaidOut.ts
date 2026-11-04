import { useLayoutEffect, useState } from 'react'

/** The size of an element along an axis, as it is laid out. */
const sizeOf = (element: HTMLElement, axis: 'height' | 'width'): number =>
    (axis === 'height' ? element.clientHeight : element.clientWidth)

/**
 * Whether an element is laid out along an axis: whether it has a height, or a width, to measure.
 *
 * A list that draws only what is in sight measures the element it scrolls in, and draws everything where there is
 * nothing to measure — on a screen not laid out yet, or in a test, where nothing is laid out at all. The answer
 * follows the element until it is laid out, so a list drawn whole draws what is in sight once it can.
 *
 * Once laid out, an element stays so: hidden — a pane folded away, a tab not shown — it has no size for a while, and a
 * list drawn whole while nobody sees it would cost what drawing only what is in sight saves, and lose what its parts
 * hold.
 *
 * An element there from the first drawing — the page itself — answers from the start. One drawn later is taken to
 * be laid out until it says otherwise.
 */
export const useLaidOut = (element: HTMLElement | null | undefined, axis: 'height' | 'width'): boolean => {
    const [laidOut, setLaidOut] = useState(() => element == null || sizeOf(element, axis) > 0)
    useLayoutEffect(() => {
        if (element == null) {
            return undefined
        }
        // Once laid out, the element is not followed any more: it stays laid out.
        if (sizeOf(element, axis) > 0) {
            setLaidOut(true)
            return undefined
        }
        setLaidOut(false)
        const observer = new ResizeObserver(() => {
            if (sizeOf(element, axis) > 0) {
                setLaidOut(true)
                observer.disconnect()
            }
        })
        observer.observe(element)
        return () => observer.disconnect()
    }, [axis, element])
    return laidOut
}
