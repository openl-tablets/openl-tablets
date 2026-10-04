import { describe, expect, it } from 'vitest'
import type { RawTableCell, RawTableCellBorderSide, RawTableCellStyle } from 'types/tables'
import { withoutFirstRows } from './hiddenRows'

const THEMED: RawTableCellBorderSide = { style: 'solid', width: 1, color: '#4472c4' }
const DASHED: RawTableCellBorderSide = { style: 'dashed', width: 1 }

/** How the theme draws a cell: with the given line under it. */
const lineUnder = (line: RawTableCellBorderSide): RawTableCellStyle => ({ border: { bottom: line }, source: 'theme' })

describe('withoutFirstRows', () => {
    it('gives the rows themselves when none is kept out of sight', () => {
        const rows: RawTableCell[][] = [[{ cell: 'A1', value: 'Rules' }]]

        expect(withoutFirstRows(rows, 0)).toBe(rows)
    })

    it('draws the line the theme draws under the rows kept out of sight on the first row left', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Rules', style: lineUnder(THEMED) }],
            [{ cell: 'A2', value: 'Age', style: { background: '#ffffff', source: 'theme' } }],
        ]

        const left = withoutFirstRows(rows, 1)

        expect(left[0]?.[0]?.style).toEqual({ background: '#ffffff', source: 'theme', border: { top: THEMED } })
    })

    it('keeps the line a cell draws on its top itself', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Rules', style: lineUnder(THEMED) }],
            [{ cell: 'A2', value: 'Age', style: { border: { top: DASHED }, source: 'theme' } }],
        ]

        expect(withoutFirstRows(rows, 1)[0]?.[0]?.style?.border).toEqual({ top: DASHED })
    })

    it('draws the line under a merged cell over every cell under it', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Rules Premium()', colspan: 2, style: lineUnder(THEMED) }, { covered: true }],
            [{ cell: 'A2', value: 'Age', style: { source: 'theme' } },
                { cell: 'B2', value: 'Premium', style: { source: 'theme' } }],
        ]

        const left = withoutFirstRows(rows, 1)

        expect(left[0]?.map(cell => cell.style?.border?.top)).toEqual([THEMED, THEMED])
    })

    it('leaves a cell a region of the rows kept out of sight covers to that region', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Group', rowspan: 2, style: lineUnder(THEMED) }],
            [{ covered: true }],
        ]

        expect(withoutFirstRows(rows, 1)).toEqual([[{ covered: true }]])
    })

    it('leaves the first row left as it is when no line is drawn over it', () => {
        const age: RawTableCell = { cell: 'A2', value: 'Age', style: { bold: true, source: 'theme' } }
        const rows: RawTableCell[][] = [[{ cell: 'A1', value: 'Rules', style: { source: 'theme' } }], [age]]

        expect(withoutFirstRows(rows, 1)[0]?.[0]).toBe(age)
    })

    it('draws nothing for a table of no more rows than it keeps out of sight', () => {
        expect(withoutFirstRows([[{ cell: 'A1', value: 'Rules' }]], 1)).toEqual([])
    })
})
