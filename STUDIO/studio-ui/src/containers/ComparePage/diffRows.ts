import type { RawTableCell } from 'types/tables'
import type { ComparisonSide, ComparisonTable } from 'types/compare'

/** The rows of one side that hold a cell reading differently in the other file. */
const changedRowsOf = (side: ComparisonSide): number[] => {
    const changed = new Set(side.changedCells ?? [])
    return side.source
        .map((row, index) => (row.some(cell => cell.cell && changed.has(cell.cell)) ? index : -1))
        .filter(index => index >= 0)
}

/**
 * The rows both files show, or null for the whole table.
 *
 * The two versions keep the same rows, whichever side a difference was found on. A row one file adds
 * is marked on that side alone, and showing it there against the whole table on the other side would
 * put the two versions out of step.
 *
 * That holds only while the versions have the same number of rows. Once they do not, the rows no
 * longer stand against each other, so both versions are shown whole. A table that differs in what it
 * is rather than in what it holds - its name, its place, its size - has no row to keep, and is shown
 * whole as well.
 */
export const rowsToShow = (table: ComparisonTable, showEqualRows: boolean): ReadonlySet<number> | null => {
    const { first, second } = table
    if (showEqualRows || !first || !second) {
        return null
    }
    if (first.source.length !== second.source.length) {
        return null
    }
    const rows = new Set([...changedRowsOf(first), ...changedRowsOf(second)])
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
