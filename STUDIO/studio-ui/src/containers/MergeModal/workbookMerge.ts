import {
    PALETTE_CONFLICT,
    type ConflictCellsDecision,
    type ConflictCellSide,
    type ConflictSide,
} from '../../services/compare'
import type { RawTableCell } from 'types/tables'
import { columnLettersOf, placeOf } from 'components/tableRoles'
import type {
    ConflictCell,
    ConflictSheet,
    ConflictTable,
    ConflictTableMerge,
    ConflictTableVersion,
    ConflictWorkbook,
} from './types'

/** How many rows around the conflicting cells of a table are shown when it is not shown whole. */
const CONTEXT_ROWS = 2

/** How many rows the rows around the conflicting cells leave out at least, or the table is shown whole. */
const MIN_HIDDEN_ROWS = 5

/** The version each conflict of a workbook is taken from, by the identifier of the conflict. */
export type WorkbookDecisions = Record<string, ConflictSide>

/**
 * How a table is merged cell by cell: the version every conflicting cell takes, the versions decided for single cells
 * and the values typed for cells, each cell named by {@link cellKey}.
 */
export interface CellChoices {
    side?: ConflictCellSide | undefined
    cells: Record<string, ConflictCellSide>
    values: Record<string, string>
}

/** How each table merged cell by cell is merged, by the identifier of its conflict. */
export type CellDecisions = Record<string, CellChoices>

/** A table merged cell by cell with nothing decided yet. */
export const NO_CHOICES: CellChoices = { cells: {}, values: {} }

/**
 * The cell the headings of a table start from, in A1 notation: the first cell of its range, as many rows down as the
 * rows shown leave out above.
 */
export const headingsFrom = (range: string, skipped: number): string => {
    const first = placeOf(range.split(':')[0]) ?? { row: 0, column: 0 }
    return `${columnLettersOf(first.column)}${first.row + skipped + 1}`
}

/** Names a cell of a merged table by its row and its column, from 0. */
export const cellKey = (row: number, column: number): string => `${row}:${column}`

/** What is decided for a table merged cell by cell, or nothing yet. */
export const choicesOf = (cellDecisions: CellDecisions, id: string): CellChoices =>
    (Object.hasOwn(cellDecisions, id) ? cellDecisions[id] : undefined) ?? NO_CHOICES

/** The version a conflicting cell takes: the one decided for the cell, or else for the table. */
export const sideOfCell = (choices: CellChoices, row: number, column: number): ConflictCellSide | undefined =>
    choices.cells[cellKey(row, column)] ?? choices.side

/**
 * The choices of a table with a version taken for some of its conflicting cells, such as the conflicting cells of a
 * row or a column. The cells give up the values typed for them.
 */
export const withCellsTaken = (choices: CellChoices, cells: ConflictCell[], side: ConflictCellSide): CellChoices => {
    const keys = new Set(cells.map(cell => cellKey(cell.row, cell.column)))
    return {
        ...choices,
        cells: { ...choices.cells, ...Object.fromEntries([...keys].map(key => [key, side])) },
        values: Object.fromEntries(Object.entries(choices.values).filter(([key]) => !keys.has(key))),
    }
}

/**
 * The choices of a table with one version taken for every conflicting cell of it. What was chosen for single cells,
 * and the values typed for conflicting cells, are given up; the values typed for the other cells stay.
 */
export const withEveryCellTaken = (choices: CellChoices, cells: ConflictCell[], side: ConflictCellSide): CellChoices =>
    ({ ...withCellsTaken(choices, cells, side), side, cells: {} })

/** The version all the conflicting cells given take, or none when they take different ones or a value is typed. */
export const sideOfAll = (choices: CellChoices, cells: ConflictCell[]): ConflictCellSide | undefined => {
    const sides = new Set(cells.map(cell => typedOf(choices, cell.row, cell.column) === undefined
        ? sideOfCell(choices, cell.row, cell.column)
        : undefined))
    return sides.size === 1 ? [...sides][0] : undefined
}

/** The choices of a table with a value typed for a cell, or with the value typed for it given up. */
export const withCellTyped = (choices: CellChoices, key: string, value: string | undefined): CellChoices => {
    const { [key]: _typed, ...values } = choices.values
    return { ...choices, values: value === undefined ? values : { ...values, [key]: value } }
}

/** The value typed for a cell of a merged table, if any. */
export const typedOf = (choices: CellChoices, row: number, column: number): string | undefined => {
    const key = cellKey(row, column)
    return Object.hasOwn(choices.values, key) ? choices.values[key] : undefined
}

/** Whether a conflicting cell has a version decided or a value typed. */
export const isDecided = (choices: CellChoices, cell: ConflictCell): boolean =>
    !!sideOfCell(choices, cell.row, cell.column) || typedOf(choices, cell.row, cell.column) !== undefined

/** The conflicting cells of a table merged cell by cell that lie past the rows of it the server sends. */
export const outOfSightOf = (merged: ConflictTableMerge): ConflictCell[] =>
    merged.conflicts.filter(cell => cell.row >= merged.cells.length)

/** How many conflicting cells of a table merged cell by cell are left without a version or a value. */
export const cellsLeftOf = (table: ConflictTable, choices: CellChoices): number =>
    (table.merged?.conflicts ?? []).filter(cell => !isDecided(choices, cell)).length

/**
 * Whether a table is merged cell by cell: it comes merged, and is not taken whole from a version.
 */
export const isByCells = (table: ConflictTable, decisions: WorkbookDecisions): boolean =>
    !!table.merged && !decisionOf(decisions, table.id)

/**
 * Whether a conflict of a sheet is resolved: taken whole, or merged cell by cell with every conflicting cell decided.
 */
const isResolved = (
    sheet: ConflictSheet,
    id: string,
    decisions: WorkbookDecisions,
    cellDecisions: CellDecisions
): boolean => {
    if (decisionOf(decisions, id)) {
        return true
    }
    const table = sheet.tables.find(item => item.id === id)
    return !!table?.merged && cellsLeftOf(table, choicesOf(cellDecisions, id)) === 0
}

/**
 * The version a conflict is taken from, or none while it is still to be resolved. A sheet may be named as anything
 * an object already answers to, such as `constructor`, so only a decision made for it counts.
 */
export const decisionOf = (decisions: WorkbookDecisions, id: string): ConflictSide | undefined =>
    Object.hasOwn(decisions, id) ? decisions[id] : undefined

/** Whether both branches changed a sheet in ways the merge cannot combine, so it is left to resolve. */
export const isConflicting = (sheet: ConflictSheet): boolean => sheet.status === 'conflict'

/** Whether a conflicting sheet is resolved only as a whole: its conflict lies outside its tables. */
export const isWhole = (sheet: ConflictSheet): boolean => !!sheet.reason || sheet.tables.length === 0

/**
 * The conflicts of a sheet, by their identifiers: the sheet itself when it is resolved only as a whole, its tables
 * otherwise, and none for a sheet the merge takes by itself.
 */
export const conflictsOf = (sheet: ConflictSheet): string[] => {
    if (!isConflicting(sheet)) {
        return []
    }
    return isWhole(sheet) ? [sheet.name] : sheet.tables.map(table => table.id)
}

/**
 * How many conflicts of a sheet are resolved: a sheet taken whole from one version resolves every one of them, and a
 * table merged cell by cell is resolved once each of its conflicting cells is decided.
 */
export const resolvedOf = (
    sheet: ConflictSheet,
    decisions: WorkbookDecisions,
    cellDecisions: CellDecisions
): number => {
    const conflicts = conflictsOf(sheet)
    return decisionOf(decisions, sheet.name)
        ? conflicts.length
        : conflicts.filter(id => isResolved(sheet, id, decisions, cellDecisions)).length
}

/** How many conflicts of a workbook are resolved, and how many it has, the palette of an .xls workbook included. */
export const progressOf = (
    workbook: ConflictWorkbook,
    decisions: WorkbookDecisions,
    cellDecisions: CellDecisions
): { resolved: number; count: number } => {
    const palette = workbook.palette ? 1 : 0
    return {
        resolved: workbook.sheets.reduce((sum, sheet) => sum + resolvedOf(sheet, decisions, cellDecisions),
            palette && decisionOf(decisions, PALETTE_CONFLICT) ? 1 : 0),
        count: workbook.sheets.reduce((sum, sheet) => sum + conflictsOf(sheet).length, palette),
    }
}

/**
 * The decisions a merge of the workbook takes: a sheet taken whole stands for its tables, so the decisions made for
 * its tables are left out, and so is anything decided that is not a conflict of the workbook. A table not taken whole
 * that comes merged is merged cell by cell as decided.
 */
export const decisionsOf = (
    workbook: ConflictWorkbook,
    decisions: WorkbookDecisions,
    cellDecisions: CellDecisions
): { whole: WorkbookDecisions; byCells: Record<string, ConflictCellsDecision> } => {
    const whole: WorkbookDecisions = {}
    const byCells: Record<string, ConflictCellsDecision> = {}
    const take = (id: string) => {
        const side = decisionOf(decisions, id)
        if (side) {
            whole[id] = side
        }
    }
    workbook.sheets.filter(isConflicting).forEach(sheet => {
        if (decisionOf(decisions, sheet.name)) {
            take(sheet.name)
            return
        }
        conflictsOf(sheet).forEach(take)
        sheet.tables.filter(table => isByCells(table, decisions))
            .forEach(table => {
                byCells[table.id] = requestOf(choicesOf(cellDecisions, table.id))
            })
    })
    if (workbook.palette) {
        take(PALETTE_CONFLICT)
    }
    return { whole, byCells }
}

/** How a table merged cell by cell is asked to be merged: the cells by their rows and columns. */
const requestOf = (choices: CellChoices): ConflictCellsDecision => {
    const keys = new Set([...Object.keys(choices.cells), ...Object.keys(choices.values)])
    return {
        ...(choices.side && { side: choices.side }),
        cells: [...keys].map(key => {
            const [row = 0, column = 0] = key.split(':').map(Number)
            const side = choices.cells[key]
            const value = typedOf(choices, row, column)
            return { row, column, ...(side && { side }), ...(value !== undefined && { value }) }
        }),
    }
}


/** The rows of a table shown around its conflicting cells, from the first to the last, counted from 0. */
export interface RowWindow {
    from: number
    to: number
}

/**
 * The rows around the cells both branches changed differently, the same in every version of the table and in the table
 * merged cell by cell: they hold the table at one size whenever such cells are known. None when the table has no such
 * cells, or a version or the merged table has another size, so the table is shown whole. A table is shown whole as
 * well when the window would leave out fewer than five rows.
 *
 * The window starts no lower than a row whose cells all begin in it, so no merged cell is cut at its top.
 */
export const conflictWindowOf = (table: ConflictTable): RowWindow | undefined => {
    const versions = [table.ours, table.base, table.theirs].filter((v): v is ConflictTableVersion => !!v)
    const grids = [...versions.map(version => version.cells), ...(table.merged ? [table.merged.cells] : [])]
    const sized = versions[0]
    if (!sized || grids.some(cells => cells.length !== sized.cells.length)) {
        return undefined
    }
    const conflicting = new Set(sized.conflictCells)
    const rows = [
        ...sized.cells.flatMap((row, index) => row.some(cell => cell.cell && conflicting.has(cell.cell))
            ? [index]
            : []),
        ...(table.merged?.conflicts ?? []).map(cell => cell.row),
    ]
    if (rows.length === 0) {
        return undefined
    }
    let from = Math.max(0, Math.min(...rows) - CONTEXT_ROWS)
    while (from > 0 && grids.some(cells => coveredFromAbove(cells, from))) {
        from--
    }
    const to = Math.min(sized.cells.length - 1, Math.max(...rows) + CONTEXT_ROWS)
    return sized.cells.length - (to - from + 1) < MIN_HIDDEN_ROWS ? undefined : { from, to }
}

/** Whether a row holds a cell covered by a merged cell that begins in a row above it. */
const coveredFromAbove = (cells: RawTableCell[][], row: number): boolean =>
    (cells[row] ?? []).some((cell, column) => {
        const above = cells[row - 1]?.[column]
        return !!cell.covered && !!above && (!!above.covered || (above.rowspan ?? 1) > 1)
    })

/** What a cell shows as text: its formula, which starts with `=`, when it has one, or its value. */
export const textOf = (cell: RawTableCell | undefined): string => {
    if (!cell) {
        return ''
    }
    return cell.formula ?? String(cell.value ?? '')
}

/** The cell of a version of a table at an address, if the version has it. */
export const cellAt = (version: ConflictTableVersion | undefined, address: string): RawTableCell | undefined =>
    version?.cells.flat().find(cell => cell.cell === address)
