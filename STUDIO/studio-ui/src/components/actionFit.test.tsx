import { render, screen } from '@testing-library/react'
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

/** A bar of six actions and the trigger of its menu, measured off screen, standing in the row beside a title. */
const Bar = ({ wholeTitle = true }: { wholeTitle?: boolean }) => {
    const { barRef, measureRef, visible } = useActionFit(6, [], { wholeTitle })
    return (
        <div ref={barRef} data-testid="bar" data-visible={visible}>
            <div ref={measureRef}>{[1, 2, 3, 4, 5, 6, 'more'].map(key => <button key={key} type="button" />)}</div>
        </div>
    )
}

describe('useActionFit', () => {
    afterEach(() => vi.restoreAllMocks())

    const layOut = (name: number) => {
        // A row of 1000 px; every part 100 px wide; the name of the title, cut short, is `name` px long in full.
        vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100)
        vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(function (this: HTMLElement) {
            return this.dataset['row'] === undefined ? 0 : 1000
        })
        vi.spyOn(HTMLElement.prototype, 'scrollWidth', 'get').mockImplementation(function (this: HTMLElement) {
            return this.dataset['name'] === undefined ? 0 : name
        })
    }

    it('keeps the whole name of the title beside the bar, and collapses actions to make room for it', () => {
        layOut(400)

        render(<div data-row><div><span data-name>A long name</span></div><Bar /></div>)

        // 1000 px less 500 for the title, which asks for its 100 shown and 400 cut, leaves room for three and More.
        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '3')
    })

    it('gives the title no more than half the row, nor less than a short name needs', () => {
        layOut(2000)
        const { unmount } = render(<div data-row><div><span data-name>A name far too long</span></div><Bar /></div>)
        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '3')
        unmount()

        layOut(0)
        render(<div data-row><Bar /></div>)
        // No title at all: the bar keeps all but the least room a title has, and every action fits.
        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '6')
    })

    it('keeps only the least room for a title whose whole name it is not asked to keep', () => {
        layOut(400)

        render(<div data-row><div><span data-name>A long name</span></div><Bar wholeTitle={false} /></div>)

        expect(screen.getByTestId('bar')).toHaveAttribute('data-visible', '6')
    })
})
