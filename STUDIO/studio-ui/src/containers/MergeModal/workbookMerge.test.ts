import {
    cellsLeftOf,
    choicesOf,
    conflictsOf,
    conflictWindowOf,
    decisionOf,
    decisionsOf,
    headingsFrom,
    isByCells,
    isWhole,
    NO_CHOICES,
    progressOf,
    resolvedOf,
    sideOfAll,
    sideOfCell,
    textOf,
    typedOf,
    withCellsTaken,
    withEveryCellTaken,
    type CellChoices,
} from './workbookMerge'
import type { ConflictCell, ConflictSheet, ConflictTable, ConflictTableMerge, ConflictWorkbook } from './types'

const table = (id: string): ConflictTable => ({ id, name: id, reason: 'cells' })

const sheet = (name: string, overrides: Partial<ConflictSheet> = {}): ConflictSheet => ({
    name,
    status: 'conflict',
    ours: true,
    base: true,
    theirs: true,
    tables: [],
    ...overrides,
})

const RULES = sheet('Rules', { tables: [table('Rules/A1:B6'), table('Rules/A9:C12')]})
const REMOVED = sheet('Removed', { reason: 'removedAndChanged', theirs: false })
const RATES = sheet('Rates', { status: 'theirs' })

const WORKBOOK: ConflictWorkbook = { sheets: [RULES, REMOVED, RATES], palette: false }

describe('workbookMerge', () => {
    it('tells the tables of a sheet as its conflicts, or the sheet itself when it is resolved only as a whole', () => {
        expect(conflictsOf(RULES)).toEqual(['Rules/A1:B6', 'Rules/A9:C12'])
        expect(conflictsOf(REMOVED)).toEqual(['Removed'])
        expect(conflictsOf(RATES)).toEqual([])
    })

    it('resolves a conflicting sheet without tables or a reason as a whole', () => {
        const bare = sheet('Bare')

        expect(isWhole(bare)).toBe(true)
        expect(isWhole(RULES)).toBe(false)
        expect(conflictsOf(bare)).toEqual(['Bare'])
    })

    it('reads only the decisions made, whatever a sheet is named', () => {
        const named = sheet('constructor', { reason: 'removedAndChanged' })
        const workbook = { sheets: [named], palette: false }

        expect(decisionOf({}, 'constructor')).toBeUndefined()
        expect(decisionOf(Object.fromEntries([['constructor', 'ours' as const]]), 'constructor')).toBe('ours')
        expect(progressOf(workbook, {}, {})).toEqual({ resolved: 0, count: 1 })
        expect(decisionsOf(workbook, {}, {})).toEqual({ whole: {}, byCells: {} })
        expect(resolvedOf(sheet('toString', { tables: [table('toString/A1:B2')]}), {}, {})).toBe(0)
    })

    it('shows the rows around the conflicting cells, the same in every version of a table', () => {
        const version = (conflictCells: string[], rows = 10) => ({
            range: `A1:A${rows}`,
            cells: Array.from({ length: rows }, (_, row) => [{ cell: `A${row + 1}`, value: row }]),
            rows,
            changedCells: [],
            formattingChanged: false,
            conflictCells,
        })
        const conflicting = { ...table('Rules/A1:A10'), ours: version(['A6']), base: version(['A6']) }

        expect(conflictWindowOf(conflicting)).toEqual({ from: 3, to: 7 })
        // A version at another size, or no conflicting cell, shows the whole table.
        expect(conflictWindowOf({ ...conflicting, theirs: version([], 11) })).toBeUndefined()
        expect(conflictWindowOf({ ...table('Rules/A1:A10'), ours: version([]) })).toBeUndefined()
        // A window that would leave out fewer than five rows is no window.
        expect(conflictWindowOf({ ...table('Rules/A1:A9'), ours: version(['A5'], 9) })).toBeUndefined()
    })

    it('starts the rows around a conflict where no merged cell is cut', () => {
        const cells = Array.from({ length: 12 }, (_, row) => [{ cell: `A${row + 1}`, value: row }]) as
            { cell?: string; value: number | null; rowspan?: number; covered?: boolean }[][]
        cells[2] = [{ cell: 'A3', value: 2, rowspan: 2 }]
        cells[3] = [{ covered: true, value: null }]
        const version = { range: 'A1:A12', cells, rows: 12, changedCells: [], formattingChanged: false,
            conflictCells: ['A6']}

        expect(conflictWindowOf({ ...table('Rules/A1:A12'), ours: version })).toEqual({ from: 2, to: 7 })
    })

    it('tells a cell by its formula, or else by its value', () => {
        expect(textOf({ cell: 'A1', value: 3, formula: '=B1+2' })).toBe('=B1+2')
        expect(textOf({ cell: 'A1', value: 3 })).toBe('3')
        expect(textOf(undefined)).toBe('')
    })

    it('counts every table of a sheet taken whole as resolved', () => {
        expect(resolvedOf(RULES, { 'Rules/A1:B6': 'theirs' }, {})).toBe(1)
        expect(resolvedOf(RULES, { Rules: 'ours' }, {})).toBe(2)
    })

    it('counts the conflicts of a workbook, the palette of an .xls workbook included', () => {
        expect(progressOf(WORKBOOK, {}, {})).toEqual({ resolved: 0, count: 3 })
        expect(progressOf(WORKBOOK, { 'Rules/A9:C12': 'base', Removed: 'theirs' }, {})).toEqual({ resolved: 2, count: 3 })
        expect(progressOf({ ...WORKBOOK, palette: true }, { '/palette': 'ours' }, {})).toEqual({ resolved: 1, count: 4 })
    })

    it('sends a sheet taken whole in place of its tables, and nothing that is no conflict', () => {
        expect(decisionsOf(WORKBOOK, {
            'Rules': 'theirs',
            'Rules/A1:B6': 'ours',
            'Removed': 'base',
            'Rates': 'ours',
            '/palette': 'theirs',
        }, {})).toEqual({ whole: { Rules: 'theirs', Removed: 'base' }, byCells: {} })
        expect(decisionsOf({ ...WORKBOOK, palette: true }, { 'Rules/A1:B6': 'ours', '/palette': 'theirs' }, {}))
            .toEqual({ whole: { 'Rules/A1:B6': 'ours', '/palette': 'theirs' }, byCells: {} })
    })

    describe('a table merged cell by cell', () => {
        const merged: ConflictTableMerge = {
            range: 'A1:B3',
            cells: [
                [{ cell: 'A1', value: 'Rules rate' }, { cell: 'B1', value: null }],
                [{ cell: 'A2', value: 'US' }, { cell: 'B2', value: 1 }],
                [{ cell: 'A3', value: 'CA' }, { cell: 'B3', value: 2 }],
            ],
            rows: 3,
            changedCells: [],
            conflicts: [
                { row: 1, column: 1, cell: 'B2', ourCell: 'B2', theirCell: 'B2',
                    ours: { cell: 'B2', value: 1.4 }, theirs: { cell: 'B2', value: 1.5 } },
                { row: 2, column: 1, cell: 'B3', ourCell: 'B3', theirCell: 'B3',
                    ours: { cell: 'B3', value: 2.4 }, theirs: { cell: 'B3', value: 2.5 } },
            ],
        }
        const rates: ConflictTable = { ...table('Rules/A1:B3'), merged }
        const rules = sheet('Rules', { tables: [rates, table('Rules/A9:C12')]})
        const choices = (overrides: Partial<CellChoices>): CellChoices => ({ ...NO_CHOICES, ...overrides })

        it('takes the version of a cell, else of the table', () => {
            const decided = choices({ side: 'ours', cells: { '2:1': 'theirs' } })

            expect(sideOfCell(decided, 2, 1)).toBe('theirs')
            expect(sideOfCell(decided, 1, 1)).toBe('ours')
            expect(sideOfCell(NO_CHOICES, 0, 0)).toBeUndefined()
        })

        it('takes a version for some conflicting cells or for every one, giving up what was typed for them', () => {
            const [first, second] = merged.conflicts as [ConflictCell, ConflictCell]
            const decided = choices({ side: 'ours', cells: { '2:1': 'ours' }, values: { '1:1': '1.45', '0:0': 'Rates' } })

            expect(withCellsTaken(decided, [first], 'theirs')).toEqual({
                side: 'ours',
                cells: { '2:1': 'ours', '1:1': 'theirs' },
                values: { '0:0': 'Rates' },
            })
            // Every conflicting cell takes the version, whatever was chosen for it; a cell that is no conflict keeps
            // the value typed for it.
            expect(withEveryCellTaken(decided, [first, second], 'theirs'))
                .toEqual({ side: 'theirs', cells: {}, values: { '0:0': 'Rates' } })
        })

        it('tells the version all the conflicting cells given take', () => {
            const [first, second] = merged.conflicts as [ConflictCell, ConflictCell]

            expect(sideOfAll(choices({ side: 'theirs' }), [first, second])).toBe('theirs')
            expect(sideOfAll(choices({ side: 'theirs', cells: { '2:1': 'ours' } }), [first, second])).toBeUndefined()
            expect(sideOfAll(choices({ side: 'theirs', values: { '1:1': '1.45' } }), [first])).toBeUndefined()
            expect(sideOfAll(NO_CHOICES, [first])).toBeUndefined()
        })

        it('heads a table from the first cell of its range, past the rows shown leave out', () => {
            expect(headingsFrom('A9:B13', 0)).toBe('A9')
            expect(headingsFrom('C3:D40', 7)).toBe('C10')
        })

        it('counts the conflicting cells left without a version or a typed value', () => {
            expect(cellsLeftOf(rates, NO_CHOICES)).toBe(2)
            expect(cellsLeftOf(rates, choices({ cells: { '1:1': 'theirs' } }))).toBe(1)
            expect(cellsLeftOf(rates, choices({ cells: { '1:1': 'theirs' }, values: { '2:1': '2.45' } }))).toBe(0)
            expect(typedOf(choices({ values: { '2:1': '' } }), 2, 1)).toBe('')
            expect(typedOf(NO_CHOICES, 2, 1)).toBeUndefined()
        })

        it('is merged cell by cell until it is taken whole', () => {
            expect(isByCells(rates, {})).toBe(true)
            expect(isByCells(rates, { 'Rules/A1:B3': 'ours' })).toBe(false)
            expect(isByCells(table('Rules/A9:C12'), {})).toBe(false)
            expect(choicesOf({}, 'constructor')).toBe(NO_CHOICES)
        })

        it('is resolved once every conflicting cell is decided', () => {
            expect(resolvedOf(rules, {}, { 'Rules/A1:B3': choices({ side: 'theirs' }) })).toBe(1)
            expect(resolvedOf(rules, {}, { 'Rules/A1:B3': choices({ cells: { '1:1': 'ours' } }) })).toBe(0)
            expect(progressOf({ sheets: [rules], palette: false }, { 'Rules/A9:C12': 'base' },
                { 'Rules/A1:B3': choices({ side: 'ours' }) })).toEqual({ resolved: 2, count: 2 })
        })

        it('sends the choices of a table merged cell by cell, the cells by their rows and columns', () => {
            const workbook = { sheets: [rules], palette: false }
            const decided = choices({
                side: 'theirs',
                cells: { '1:1': 'ours' },
                values: { '1:1': '1.45', '0:0': 'Rates' },
            })

            expect(decisionsOf(workbook, { 'Rules/A9:C12': 'base' }, { 'Rules/A1:B3': decided })).toEqual({
                whole: { 'Rules/A9:C12': 'base' },
                byCells: {
                    'Rules/A1:B3': {
                        side: 'theirs',
                        cells: [
                            { row: 1, column: 1, side: 'ours', value: '1.45' },
                            { row: 0, column: 0, value: 'Rates' },
                        ],
                    },
                },
            })
            // A table taken whole, or on a sheet taken whole, sends no choices of its cells.
            expect(decisionsOf(workbook, { 'Rules/A1:B3': 'ours' }, { 'Rules/A1:B3': decided }).byCells).toEqual({})
            expect(decisionsOf(workbook, { Rules: 'ours' }, { 'Rules/A1:B3': decided }))
                .toEqual({ whole: { Rules: 'ours' }, byCells: {} })
        })

        it('shows the rows around the conflicts only while the merged table has the size of the versions', () => {
            const version = (rows: number) => ({
                range: `A1:A${rows}`,
                cells: Array.from({ length: rows }, (_, row) => [{ cell: `A${row + 1}`, value: row }]),
                rows,
                changedCells: [],
                formattingChanged: false,
                conflictCells: [],
            })
            const cells = version(10).cells
            const tall = (mergedRows: number): ConflictTable => ({
                ...table('Rules/A1:A10'),
                ours: version(10),
                base: version(10),
                merged: { range: 'A1:A10', cells: cells.slice(0, mergedRows), rows: mergedRows, changedCells: [],
                    conflicts: [{ row: 5, column: 0, cell: 'A6', ourCell: 'A6', theirCell: 'A6', ours: {}, theirs: {} }]},
            })

            expect(conflictWindowOf(tall(10))).toEqual({ from: 3, to: 7 })
            expect(conflictWindowOf(tall(9))).toBeUndefined()
        })
    })
})
