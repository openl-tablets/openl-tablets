import { PALETTE_CONFLICT, type ConflictSide } from '../../services/compare'
import type { RawTableCell } from 'types/tables'
import type { ConflictSheet, ConflictTable, ConflictTableVersion, ConflictWorkbook } from './types'

/** How many rows around the conflicting cells of a table are shown when it is not shown whole. */
const CONTEXT_ROWS = 2

/** How many rows the rows around the conflicting cells leave out at least, or the table is shown whole. */
const MIN_HIDDEN_ROWS = 5

/** The version each conflict of a workbook is taken from, by the identifier of the conflict. */
export type WorkbookDecisions = Record<string, ConflictSide>

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

/** How many conflicts of a sheet are resolved: a sheet taken whole from one version resolves every one of them. */
export const resolvedOf = (sheet: ConflictSheet, decisions: WorkbookDecisions): number => {
    const conflicts = conflictsOf(sheet)
    return decisionOf(decisions, sheet.name)
        ? conflicts.length
        : conflicts.filter(id => decisionOf(decisions, id)).length
}

/** How many conflicts of a workbook are resolved, and how many it has, the palette of an .xls workbook included. */
export const progressOf = (
    workbook: ConflictWorkbook,
    decisions: WorkbookDecisions
): { resolved: number; count: number } => {
    const palette = workbook.palette ? 1 : 0
    return {
        resolved: workbook.sheets.reduce((sum, sheet) => sum + resolvedOf(sheet, decisions),
            palette && decisionOf(decisions, PALETTE_CONFLICT) ? 1 : 0),
        count: workbook.sheets.reduce((sum, sheet) => sum + conflictsOf(sheet).length, palette),
    }
}

/**
 * The decisions a merge of the workbook takes: a sheet taken whole stands for its tables, so the decisions made for
 * its tables are left out, and so is anything decided that is not a conflict of the workbook.
 */
export const decisionsOf = (workbook: ConflictWorkbook, decisions: WorkbookDecisions): WorkbookDecisions => {
    const taken: WorkbookDecisions = {}
    const take = (id: string) => {
        const side = decisionOf(decisions, id)
        if (side) {
            taken[id] = side
        }
    }
    workbook.sheets.filter(isConflicting).forEach(sheet => {
        if (decisionOf(decisions, sheet.name)) {
            take(sheet.name)
        } else {
            conflictsOf(sheet).forEach(take)
        }
    })
    if (workbook.palette) {
        take(PALETTE_CONFLICT)
    }
    return taken
}

/** The rows of a table shown around its conflicting cells, from the first to the last, counted from 0. */
export interface RowWindow {
    from: number
    to: number
}

/**
 * The rows around the cells both branches changed differently, the same in every version of the table: they hold the
 * table at one size whenever such cells are known. None when the table has no such cells, or a version has the table
 * at another size, so the table is shown whole. A table is shown whole as well when the window would leave out fewer
 * than five rows.
 *
 * The window starts no lower than a row whose cells all begin in it, so no merged cell is cut at its top.
 */
export const conflictWindowOf = (table: ConflictTable): RowWindow | undefined => {
    const versions = [table.ours, table.base, table.theirs].filter((v): v is ConflictTableVersion => !!v)
    const sized = versions[0]
    if (!sized || versions.some(version => version.cells.length !== sized.cells.length)) {
        return undefined
    }
    const conflicting = new Set(sized.conflictCells)
    const rows = sized.cells.flatMap((row, index) => row.some(cell => cell.cell && conflicting.has(cell.cell))
        ? [index]
        : [])
    if (rows.length === 0) {
        return undefined
    }
    let from = Math.max(0, Math.min(...rows) - CONTEXT_ROWS)
    while (from > 0 && versions.some(version => coveredFromAbove(version.cells, from))) {
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
