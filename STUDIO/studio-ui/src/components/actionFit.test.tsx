import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fitActionCount, useActionFit } from './actionFit'

describe('fitActionCount', () => {
    it('shows every action when they all fit', () => {
        expect(fitActionCount([50, 50, 50], 40, 500, 8)).toBe(3)
    })

    it('reserves room for the overflow trigger once something spills', () => {
        // 50 + 8 + 8 + 40 = 106 <= 110 fits one; a second (50 + 8) would need 164.
        expect(fitActionCount([50, 50, 50], 40, 110, 8)).toBe(1)
    })

    it('always keeps at least the first action, however tight the space', () => {
        expect(fitActionCount([200, 50], 40, 30, 8)).toBe(1)
    })

    it('returns zero only when there are no actions', () => {
        expect(fitActionCount([], 40, 500, 8)).toBe(0)
    })
})

/** A bar of six actions and the trigger of its menu, measured off screen, standing in its row. */
const Bar = () => {
    const { barRef, measureRef, visible } = useActionFit(6, [])
    return (
        <div ref={barRef} data-testid="bar" data-visible={visible}>
            <div ref={measureRef}>{[1, 2, 3, 4, 5, 6, 'more'].map(key => <button key={key} type="button" />)}</div>
        </div>
    )
}

describe('useActionFit', () => {
    afterEach(() => vi.restoreAllMocks())

    /** A row `row` px wide; every part 100 px wide. */
    const layOut = (row: number) => {
        vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100)
        vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(function (this: HTMLElement) {
            return this.dataset['row'] === undefined ? 0 : row
        })
    }

    it('shows every action the row has room for', () => {
        layOut(1000)

        render(<div data-row><Bar /></div>)

        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '6')
    })

    it('collapses the actions the row has no room for, keeping room for the menu', () => {
        // Six actions and their gaps take 640 px: 500 px holds three and the trigger of the menu.
        layOut(500)
        render(<div data-row><Bar /></div>)
        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '3')
    })

    it('works the fit out again when an action grows in place, the row keeping its width', () => {
        const observed: Element[] = []
        let resized = () => {}
        vi.stubGlobal('ResizeObserver', class {
            constructor(callback: () => void) {
                resized = callback
            }

            observe(target: Element) {
                observed.push(target)
            }

            disconnect() {}
        })
        try {
            layOut(500)
            render(<div data-row><Bar /></div>)
            expect(observed).toHaveLength(2)

            // A count shows on every button: 150 px each, so two and the trigger of the menu fit.
            vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(150)
            act(() => resized())

            expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '2')
        } finally {
            vi.unstubAllGlobals()
        }
    })
})
