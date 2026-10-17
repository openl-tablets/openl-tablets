import {
    conflictsOf,
    conflictWindowOf,
    decisionOf,
    decisionsOf,
    isWhole,
    progressOf,
    resolvedOf,
    textOf,
} from './workbookMerge'
import type { ConflictSheet, ConflictTable, ConflictWorkbook } from './types'

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
        expect(progressOf(workbook, {})).toEqual({ resolved: 0, count: 1 })
        expect(decisionsOf(workbook, {})).toEqual({})
        expect(resolvedOf(sheet('toString', { tables: [table('toString/A1:B2')]}), {})).toBe(0)
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
        expect(resolvedOf(RULES, { 'Rules/A1:B6': 'theirs' })).toBe(1)
        expect(resolvedOf(RULES, { Rules: 'ours' })).toBe(2)
    })

    it('counts the conflicts of a workbook, the palette of an .xls workbook included', () => {
        expect(progressOf(WORKBOOK, {})).toEqual({ resolved: 0, count: 3 })
        expect(progressOf(WORKBOOK, { 'Rules/A9:C12': 'base', Removed: 'theirs' })).toEqual({ resolved: 2, count: 3 })
        expect(progressOf({ ...WORKBOOK, palette: true }, { '/palette': 'ours' })).toEqual({ resolved: 1, count: 4 })
    })

    it('sends a sheet taken whole in place of its tables, and nothing that is no conflict', () => {
        expect(decisionsOf(WORKBOOK, {
            'Rules': 'theirs',
            'Rules/A1:B6': 'ours',
            'Removed': 'base',
            'Rates': 'ours',
            '/palette': 'theirs',
        })).toEqual({ Rules: 'theirs', Removed: 'base' })
        expect(decisionsOf({ ...WORKBOOK, palette: true }, { 'Rules/A1:B6': 'ours', '/palette': 'theirs' }))
            .toEqual({ 'Rules/A1:B6': 'ours', '/palette': 'theirs' })
    })
})
