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
            [{ from: '100', to: '200', fromIncluded: true, toIncluded: true }, '[100..200]'],
            [{ from: '100', to: '200', fromIncluded: false, toIncluded: true }, '(100..200]'],
            [{ from: '500', to: '', fromIncluded: true, toIncluded: true }, '>= 500'],
            [{ from: '500', to: '', fromIncluded: false, toIncluded: true }, '> 500'],
            [{ from: '', to: '14', fromIncluded: true, toIncluded: true }, '<= 14'],
            [{ from: '', to: '14', fromIncluded: true, toIncluded: false }, '< 14'],
            // One value standing for itself is written as that value, the way OpenL prints it.
            [{ from: '100', to: '100', fromIncluded: true, toIncluded: true }, '100'],
            [NO_RANGE, ''],
        ])('writes %o as %s', (bounds, expected) => {
            expect(formatRange(bounds)).toBe(expected)
        })

        it('writes back what it read, for every form OpenL prints', () => {
            ['[100..200]', '(100..200)', '>= 500', '< 14', '100'].forEach(written =>
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
