import { describe, expect, it } from 'vitest'
import type { ComparisonLine, ComparisonLineStatus } from 'types/compare'
import type { RawTableCell } from 'types/tables'
import type { Alignment } from './alignment'
import { keepRows, rowsToShow, type ShownLine } from './diffRows'

/** An alignment of one column whose rows are as named, each paired with the row of the same place. */
const alignmentOf = (...statuses: ComparisonLineStatus[]): Alignment => ({
    rows: statuses.map((status, line): ComparisonLine => ({ first: line, second: line, status })),
    columns: [{ first: 0, second: 0, status: 'equal' }],
})

const EQUAL = 'equal' as const

describe('rowsToShow', () => {
    it('shows the header, the rows that differ and an equal row on either side, the rest a gap', () => {
        const alignment = alignmentOf(EQUAL, EQUAL, EQUAL, EQUAL, 'added', EQUAL, EQUAL, EQUAL, EQUAL, 'changed')

        expect(rowsToShow(alignment, false)).toEqual([
            { line: 0 },
            { gap: 2 },
            { line: 3 }, { line: 4 }, { line: 5 },
            { gap: 2 },
            { line: 8 }, { line: 9 },
        ])
    })

    it('shows a single equal row rather than a gap in its place', () => {
        const alignment = alignmentOf(EQUAL, EQUAL, EQUAL, 'added', EQUAL, EQUAL, EQUAL, 'removed', EQUAL, EQUAL)

        // Row 1 stands between the header and row 2, row 5 between rows 4 and 6, row 9 alone at the end.
        expect(rowsToShow(alignment, false)).toEqual(Array.from({ length: 10 }, (_, line) => ({ line })))
    })

    it('ends with a gap for the equal rows after the last one that differs', () => {
        const alignment = alignmentOf(EQUAL, 'changed', EQUAL, EQUAL, EQUAL, EQUAL)

        expect(rowsToShow(alignment, false)).toEqual([{ line: 0 }, { line: 1 }, { line: 2 }, { gap: 3 }])
    })

    it('shows every row when the equal ones are asked for, a column differs or no row differs', () => {
        const alignment = alignmentOf(EQUAL, 'changed', EQUAL, EQUAL, EQUAL)

        expect(rowsToShow(alignment, true)).toBeNull()
        expect(rowsToShow({ ...alignment, columns: [{ first: 0, status: 'removed' }]}, false)).toBeNull()
        expect(rowsToShow(alignmentOf(EQUAL, EQUAL), false)).toBeNull()
        expect(rowsToShow(null, false)).toBeNull()
    })
})

describe('keepRows', () => {
    const values = (rows: RawTableCell[][]) => rows.map(row => row.map(cell => (cell.covered ? '^' : cell.value ?? '')))

    it('puts a gap across the table where rows are left out', () => {
        const source: RawTableCell[][] = [
            [{ value: 'a' }, { value: '1' }],
            [{ value: 'b' }, { value: '2' }],
            [{ value: 'c' }, { value: '3' }],
            [{ value: 'd' }, { value: '4' }],
        ]
        const shown: ShownLine[] = [{ line: 0 }, { gap: 2 }, { line: 3 }]

        const { rows, gaps } = keepRows(source, shown, 2)

        expect(values(rows)).toEqual([['a', '1'], ['', '^'], ['d', '4']])
        expect(rows[1]![0]!.colspan).toBe(2)
        expect(gaps.get(rows[1]![0]!)).toBe(2)
        expect(gaps.size).toBe(1)
    })

    it('draws a merge a gap cuts through on either side of it, its value in each part', () => {
        const source: RawTableCell[][] = [
            [{ value: 'group', rowspan: 5 }, { value: '1' }],
            [{ covered: true }, { value: '2' }],
            [{ covered: true }, { value: '3' }],
            [{ covered: true }, { value: '4' }],
            [{ covered: true }, { value: '5' }],
        ]
        const shown: ShownLine[] = [{ line: 0 }, { line: 1 }, { gap: 2 }, { line: 4 }]

        const { rows } = keepRows(source, shown, 2)

        expect(values(rows)).toEqual([['group', '1'], ['^', '2'], ['', '^'], ['group', '5']])
        expect(rows[0]![0]!.rowspan).toBe(2)
        // The part after the gap is one row, so it is merged with no other.
        expect(rows[3]![0]!.rowspan).toBeUndefined()
    })
})
