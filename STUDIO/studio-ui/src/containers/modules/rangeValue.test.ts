import { describe, expect, it } from 'vitest'
import { formatRange, NO_RANGE, parseRange, rangeProblem } from './rangeValue'

describe('rangeValue', () => {
    describe('reading what a cell holds', () => {
        it.each([
            ['[100..200]', { from: '100', to: '200', fromIncluded: true, toIncluded: true }],
            ['(100..200)', { from: '100', to: '200', fromIncluded: false, toIncluded: false }],
            ['[100..200)', { from: '100', to: '200', fromIncluded: true, toIncluded: false }],
            ['200..300', { from: '200', to: '300', fromIncluded: true, toIncluded: true }],
            ['200...300', { from: '200', to: '300', fromIncluded: false, toIncluded: false }],
            ['100-200', { from: '100', to: '200', fromIncluded: true, toIncluded: true }],
            ['>= 500', { from: '500', to: '', fromIncluded: true, toIncluded: true }],
            ['>500', { from: '500', to: '', fromIncluded: false, toIncluded: true }],
            ['<= 14', { from: '', to: '14', fromIncluded: true, toIncluded: true }],
            ['<14', { from: '', to: '14', fromIncluded: true, toIncluded: false }],
            ['500+', { from: '500', to: '', fromIncluded: true, toIncluded: true }],
            ['100', { from: '100', to: '100', fromIncluded: true, toIncluded: true }],
            ['-2.5..3', { from: '-2.5', to: '3', fromIncluded: true, toIncluded: true }],
        ])('reads %s', (text, expected) => {
            expect(parseRange(text)).toEqual(expected)
        })

        it('holds no bounds for text that is not a range at all', () => {
            expect(parseRange('= 2 * numberOfEmployees')).toEqual(NO_RANGE)
            expect(parseRange('')).toEqual(NO_RANGE)
        })
    })

    describe('writing the bounds back', () => {
        it.each([
            // Bounds that are both inside the range need no brackets to say so, as the Editor wrote them.
            [{ from: '100', to: '200', fromIncluded: true, toIncluded: true }, '100 .. 200'],
            [{ from: '100', to: '200', fromIncluded: false, toIncluded: true }, '(100 .. 200]'],
            [{ from: '100', to: '200', fromIncluded: true, toIncluded: false }, '[100 .. 200)'],
            [{ from: '500', to: '', fromIncluded: true, toIncluded: true }, '>=500'],
            [{ from: '500', to: '', fromIncluded: false, toIncluded: true }, '>500'],
            [{ from: '', to: '14', fromIncluded: true, toIncluded: true }, '<=14'],
            [{ from: '', to: '14', fromIncluded: true, toIncluded: false }, '<14'],
            // One value standing for itself is written as that value, the way OpenL prints it.
            [{ from: '100', to: '100', fromIncluded: true, toIncluded: true }, '100'],
            [NO_RANGE, ''],
        ])('writes %o as %s', (bounds, expected) => {
            expect(formatRange(bounds)).toBe(expected)
        })

        it.each([
            ['3+', { from: '4', to: '', fromIncluded: true, toIncluded: true }, '4+'],
            ['1-5', { from: '2', to: '6', fromIncluded: true, toIncluded: true }, '2-6'],
            ['1 ... 5', { from: '1', to: '7', fromIncluded: false, toIncluded: false }, '1 ... 7'],
            ['[1; 5)', { from: '2', to: '5', fromIncluded: true, toIncluded: true }, '[2; 5]'],
            ['(1 .. 5)', { from: '1', to: '9', fromIncluded: false, toIncluded: false }, '(1 .. 9)'],
        ])('keeps the way %s was written for %o', (written, bounds, expected) => {
            expect(formatRange(bounds, written)).toBe(expected)
        })

        it.each([
            // The author's way cannot say the new shape, or what is inside it, so the Editor's way is written.
            ['3+', { from: '4', to: '', fromIncluded: false, toIncluded: true }, '>4'],
            ['3+', { from: '1', to: '4', fromIncluded: true, toIncluded: true }, '1 .. 4'],
            ['1-5', { from: '1', to: '5', fromIncluded: false, toIncluded: true }, '(1 .. 5]'],
            ['1-5', { from: '', to: '5', fromIncluded: true, toIncluded: true }, '<=5'],
            ['= x', { from: '1', to: '5', fromIncluded: true, toIncluded: true }, '1 .. 5'],
        ])('writes %s the Editor\'s way when its way no longer fits %o', (written, bounds, expected) => {
            expect(formatRange(bounds, written)).toBe(expected)
        })

        it('writes back what it read, for every form the Editor wrote', () => {
            ['100 .. 200', '(100 .. 200)', '[100 .. 200)', '>=500', '<14', '100'].forEach(written =>
                expect(formatRange(parseRange(written))).toBe(written))
        })
    })

    describe('what is not a range', () => {
        it('refuses bounds that name nothing', () => {
            expect(rangeProblem(NO_RANGE)).toBe('empty')
        })

        it('refuses a lower bound above the upper one', () => {
            expect(rangeProblem({ from: '300', to: '200', fromIncluded: true, toIncluded: true }))
                .toBe('inverted')
        })

        it('takes a range with one bound', () => {
            expect(rangeProblem({ ...NO_RANGE, from: '500' })).toBeNull()
        })
    })
})
