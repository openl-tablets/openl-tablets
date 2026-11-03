import type { ComparisonTable } from 'types/compare'
import type { RawTableCell } from 'types/tables'
import { alignmentOf, alignSide, ownStatusOf, wholeMerges, type Alignment } from './alignment'

const TABLE: ComparisonTable = {
    id: '0-0',
    name: 'Rules',
    status: 'changed',
    first: {
        source: [[{ cell: 'A1', value: 'a' }], [{ cell: 'A2', value: 'b' }]],
        changedCells: ['A2'],
    },
    second: {
        source: [[{ cell: 'A1', value: 'a' }], [{ cell: 'A2', value: 'B' }], [{ cell: 'A3', value: 'c' }]],
        changedCells: ['A2'],
    },
}

const values = (rows: RawTableCell[][]) => rows.map(row => row.map(cell => (cell.covered ? '^' : cell.value ?? '')))

describe('alignment', () => {
    it('pairs the lines in their order when the comparison did not line them up', () => {
        const alignment = alignmentOf(TABLE)

        expect(alignment!.rows).toEqual([
            { first: 0, second: 0, status: 'equal' },
            { first: 1, second: 1, status: 'changed' },
            { second: 2, status: 'added' },
        ])
        expect(alignment!.columns).toEqual([{ first: 0, second: 0, status: 'equal' }])
    })

    it('takes the lines as the comparison lined them up', () => {
        const rows = [{ first: 0, second: 0, status: 'equal' as const }, { first: 1, status: 'removed' as const }]

        expect(alignmentOf({ ...TABLE, rows })!.rows).toBe(rows)
    })

    it('lines up nothing for a table only one file holds', () => {
        const { second: _absent, ...removed } = TABLE

        expect(alignmentOf({ ...removed, status: 'removed' })).toBeNull()
    })

    it('stands an empty line against a line only the other file holds', () => {
        const alignment: Alignment = {
            rows: [{ first: 0, second: 0, status: 'equal' }, { second: 1, status: 'added' }, { first: 1, second: 2, status: 'equal' }],
            columns: [{ first: 0, second: 0, status: 'equal' }, { second: 1, status: 'added' }],
        }
        const grid = alignSide([[{ cell: 'A1', value: 'a' }], [{ cell: 'A2', value: 'b' }]], alignment, 'first')

        expect(values(grid)).toEqual([['a', ''], ['', ''], ['b', '']])
    })

    it('stretches a merge over the empty lines inside it', () => {
        const alignment: Alignment = {
            rows: [{ first: 0, second: 0, status: 'equal' }],
            columns: [{ first: 0, second: 0, status: 'equal' }, { second: 1, status: 'added' }, { first: 1, second: 2, status: 'equal' }],
        }
        const grid = alignSide([[{ cell: 'A1', value: 'header', colspan: 2 }, { covered: true }]], alignment, 'first')

        expect(grid[0]![0]!.colspan).toBe(3)
        expect(values(grid)).toEqual([['header', '^', '^']])
    })

    it('undoes a merge that would cover a cell it does not hold', () => {
        const rows = wholeMerges([
            [{ cell: 'A1', value: 'a', colspan: 2 }, { cell: 'B1', value: 'b' }],
            [{ covered: true }, { cell: 'B2', value: 'c' }],
        ])

        expect(rows[0]![0]!.colspan).toBeUndefined()
        expect(values(rows)).toEqual([['a', 'b'], ['', 'c']])
    })

    it('keeps a merge that covers its own cells', () => {
        const rows = wholeMerges([[{ cell: 'A1', value: 'a', colspan: 2 }, { covered: true }]])

        expect(rows[0]![0]!.colspan).toBe(2)
    })

    it('tells the status of a line only one file holds', () => {
        expect(ownStatusOf('first')).toBe('removed')
        expect(ownStatusOf('second')).toBe('added')
    })

})
