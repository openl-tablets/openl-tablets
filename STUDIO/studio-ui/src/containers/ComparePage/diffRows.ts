import type { RawTableCell } from 'types/tables'
import type { Alignment } from './alignment'

/**
 * The rows lined up that differ, or null for the whole table.
 *
 * A row differs when it is added, removed or holds a changed cell. Every row is kept once a column is added or
 * removed, because such a column runs through the equal rows too. A table with no row that differs is shown
 * whole.
 */
export const rowsToShow = (alignment: Alignment | null, showEqualRows: boolean): ReadonlySet<number> | null => {
    const columnsDiffer = alignment?.columns.some(column => column.status === 'added' || column.status === 'removed')
    if (showEqualRows || !alignment || columnsDiffer) {
        return null
    }
    const rows = new Set(alignment.rows.flatMap((row, index) => (row.status === 'equal' ? [] : [index])))
    return rows.size === 0 ? null : rows
}

/**
 * The table with only the given rows, and the merges they cut through made whole again.
 *
 * A merge that starts in a row that is gone starts in the first row that is left, and every merge
 * spans only the rows that are left. Without that a cell would keep a merge reaching rows that are no
 * longer drawn, and the columns after it would move.
 */
export const keepRows = (source: RawTableCell[][], keep: ReadonlySet<number>): RawTableCell[][] => {
    const rows = source.map(row => row.map(cell => ({ ...cell })))
    // The merges are read from the table as it stands and written into the copy. Reading the copy would
    // meet a cell that has just been moved to a row further down and narrow it a second time.
    source.forEach((row, index) => row.forEach((cell, column) => {
        const span = cell.rowspan ?? 1
        if (cell.covered || span <= 1) {
            return
        }
        const kept = Array.from({ length: span }, (_, offset) => index + offset).filter(row => keep.has(row))
        if (kept.length === 0) {
            return
        }
        const [head, ...covered] = kept as [number, ...number[]]
        rows[head]![column] = { ...cell, rowspan: kept.length }
        covered.forEach(row => {
            rows[row]![column] = { covered: true }
        })
    }))
    return rows.filter((_, index) => keep.has(index))
}
