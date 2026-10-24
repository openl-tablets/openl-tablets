import { describe, expect, it } from 'vitest'
import { joinMarks, markedRuns, markInksOf, markRange, marksOf, sameMarks } from './textMarks'

const INKS = markInksOf({
    colorErrorText: '#b00000',
    colorWarningText: '#8a5a00',
    colorSuccessText: '#006400',
    colorInfoText: '#00508c',
}, ['#ffffff'])

describe('textMarks', () => {
    it('reads a mark from a piece bold in one of the colours of the marks', () => {
        expect(marksOf([
            { text: 'Good ' },
            { text: 'Morning', style: { bold: true, color: '#FF0000' } },
            { text: ' and', style: { color: '#ff0000' } },
            { text: ' day', style: { bold: true, color: '#123456' } },
        ])).toEqual([{ from: 5, to: 12, color: 'error' }])
    })

    it('reads no marks from a text in the font of its cell', () => {
        expect(marksOf(undefined)).toEqual([])
    })

    it('joins adjacent marks of one colour and drops empty ones', () => {
        expect(joinMarks([
            { from: 0, to: 2, color: 'info' },
            { from: 2, to: 4, color: 'info' },
            { from: 4, to: 4, color: 'error' },
            { from: 4, to: 6, color: 'success' },
        ])).toEqual([{ from: 0, to: 4, color: 'info' }, { from: 4, to: 6, color: 'success' }])
    })

    it('marks a piece over the marks it covers, splitting the ones it cuts', () => {
        expect(markRange([{ from: 0, to: 10, color: 'error' }], 3, 6, 'warning')).toEqual([
            { from: 0, to: 3, color: 'error' },
            { from: 3, to: 6, color: 'warning' },
            { from: 6, to: 10, color: 'error' },
        ])
    })

    it('takes the marks off a piece', () => {
        expect(markRange([{ from: 0, to: 4, color: 'error' }, { from: 6, to: 9, color: 'info' }], 2, 7, null))
            .toEqual([{ from: 0, to: 2, color: 'error' }, { from: 7, to: 9, color: 'info' }])
    })

    it('lays no mark over an empty piece', () => {
        expect(markRange([], 3, 3, 'error')).toEqual([])
    })

    it('tells marks that mark the same pieces apart from others', () => {
        const marks = [{ from: 0, to: 4, color: 'error' as const }]
        expect(sameMarks(marks, [{ from: 0, to: 4, color: 'error' }])).toBe(true)
        expect(sameMarks(marks, [{ from: 0, to: 4, color: 'info' }])).toBe(false)
        expect(sameMarks(marks, [])).toBe(false)
    })

    it('draws the marks over the pieces of highlighted code', () => {
        const code = [{ text: '= a', style: { color: '#111' } }, { text: ' + b' }]
        expect(markedRuns('= a + b', code, [{ from: 2, to: 5, color: 'success' }], INKS)).toEqual([
            { text: '= ', style: { color: '#111' } },
            { text: 'a', style: { bold: true, color: '#006400' }, mark: 'success' },
            { text: ' +', style: { bold: true, color: '#006400' }, mark: 'success' },
            { text: ' b' },
        ])
    })

    it('draws the marks of a text in the font of its cell', () => {
        expect(markedRuns('Good Morning', undefined, [{ from: 0, to: 4, color: 'info' }], INKS)).toEqual([
            { text: 'Good', style: { bold: true, color: '#00508c' }, mark: 'info' },
            { text: ' Morning' },
        ])
    })
})
