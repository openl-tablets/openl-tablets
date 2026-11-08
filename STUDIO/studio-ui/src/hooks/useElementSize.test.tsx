import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useElementSize } from './useElementSize'

/** The observer the hook made, which the test drives as a browser would. */
const observers: Array<{ callback: ResizeObserverCallback, disconnect: () => void }> = []

class FakeResizeObserver {
    readonly disconnect = vi.fn()
    constructor(readonly callback: ResizeObserverCallback) {
        observers.push(this)
    }
    observe() {
        // The test reports the sizes itself.
    }
    unobserve() {
        // Nothing is observed for real.
    }
}

const Measured = () => {
    const { ref, size } = useElementSize<HTMLDivElement>()
    return <div ref={ref} data-testid="measured">{`${size.width}x${size.height}`}</div>
}

describe('useElementSize', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        observers.length = 0
    })

    it('follows the size the element is laid out at, and stops when it goes', () => {
        vi.stubGlobal('ResizeObserver', FakeResizeObserver)
        const { unmount } = render(<Measured />)
        expect(screen.getByTestId('measured')).toHaveTextContent('0x0')

        act(() => observers[0]?.callback([{ contentRect: { width: 640.7, height: 480.2 } } as ResizeObserverEntry],
            observers[0] as unknown as ResizeObserver))
        expect(screen.getByTestId('measured')).toHaveTextContent('640x480')

        unmount()
        expect(observers[0]?.disconnect).toHaveBeenCalled()
    })
})
