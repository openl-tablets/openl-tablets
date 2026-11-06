import { useEffect, useState } from 'react'

/**
 * The size an element is laid out at, kept up to date as it changes. A virtual list draws the rows that fit and no
 * more, so it has to be told what fits.
 *
 * <p>The element is measured from the moment it is drawn, which may be later than the component that asks: the
 * hook hands out a ref to put on it.
 */
export const useElementSize = <T extends HTMLElement>() => {
    const [element, setElement] = useState<T | null>(null)
    const [size, setSize] = useState({ height: 0, width: 0 })
    useEffect(() => {
        if (element === null) {
            return undefined
        }
        const observer = new ResizeObserver(entries => {
            const box = entries[0]?.contentRect
            setSize({ height: Math.floor(box?.height ?? 0), width: Math.floor(box?.width ?? 0) })
        })
        observer.observe(element)
        return () => observer.disconnect()
    }, [element])
    return { ref: setElement, size }
}
