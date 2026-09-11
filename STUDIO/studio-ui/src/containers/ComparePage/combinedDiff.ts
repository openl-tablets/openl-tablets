import type { RawTableCell } from 'types/tables'
import type { ComparisonSide, ComparisonTable } from 'types/compare'
import { keepRows } from './diffRows'

/** What the combined view says about a row: how the second file arrived at it. */
export type RowKind = 'equal' | 'changed' | 'added' | 'removed'

/** What the combined view says about one cell of such a row. */
export interface CellMark {
    row: RowKind
    /** The value of the first file, when the two files read the cell differently. */
    before?: string
    /** The value of the second file, beside which the first one is shown. */
    after?: string
    /** The cell leads its row and carries the sign the row is read by rather than a value. */
    lead?: boolean
}

/** The two versions drawn as one table, and what each of its cells stands for. */
export interface CombinedTable {
    rows: RawTableCell[][]
    marks: Map<RawTableCell, CellMark>
}

const text = (cell: RawTableCell | undefined): string => (cell?.value == null ? '' : String(cell.value))

/** Whether the row holds a cell the file marks as read differently. */
const holdsChange = (row: RawTableCell[] | undefined, changed: ReadonlySet<string>): boolean =>
    !!row?.some(cell => cell.cell && changed.has(cell.cell))

/**
 * The cell leading a row of the combined view. It carries the row's own address, because the rows of
 * the two files meet in one table and the address a cell has in its own file is not unique there.
 */
const leadOf = (into: CombinedTable): RawTableCell => ({ cell: `#${into.rows.length}` })

/** The rows of one side, each led by the sign the whole side is read by. */
const oneSided = (side: ComparisonSide | undefined, kind: RowKind, into: CombinedTable): void => {
    side?.source.forEach(row => {
        const lead = leadOf(into)
        into.marks.set(lead, { row: kind, lead: true })
        row.forEach(cell => into.marks.set(cell, { row: kind }))
        into.rows.push([lead, ...row])
    })
}

const stacked = (first: ComparisonSide | undefined, second: ComparisonSide | undefined): CombinedTable => {
    const combined: CombinedTable = { rows: [], marks: new Map() }
    oneSided(first, 'removed', combined)
    oneSided(second, 'added', combined)
    return combined
}

const aligned = (
    first: ComparisonSide,
    second: ComparisonSide,
    keep: ReadonlySet<number> | null
): CombinedTable => {
    const combined: CombinedTable = { rows: [], marks: new Map() }
    const changedFirst = new Set(first.changedCells ?? [])
    const changedSecond = new Set(second.changedCells ?? [])
    const was = keep ? keepRows(first.source, keep) : first.source
    const now = keep ? keepRows(second.source, keep) : second.source

    now.forEach((row, index) => {
        const kind: RowKind = holdsChange(row, changedSecond) || holdsChange(was[index], changedFirst)
            ? 'changed'
            : 'equal'
        const lead = leadOf(combined)
        combined.marks.set(lead, { row: kind, lead: true })
        row.forEach((cell, column) => {
            const differs = !!cell.cell && changedSecond.has(cell.cell)
            combined.marks.set(cell, differs
                ? { row: kind, before: text(was[index]?.[column]), after: text(cell) }
                : { row: kind })
        })
        combined.rows.push([lead, ...row])
    })
    return combined
}

/**
 * The two versions of one table drawn as a single one.
 *
 * Where the two files read a cell differently, the cell carries both values - what the first file has
 * and what the second one has in its place. Every row is led by a sign saying what became of it, so a
 * row that changed is found without reading one table against another.
 *
 * The two files are put against each other row by row while they hold the same number of rows. Once
 * they do not, there is nothing to put against anything: what the first file holds is shown as dropped
 * and what the second one holds as added, one after the other, as a diff reads a block it cannot align.
 *
 * @param table the table as the comparison reports it
 * @param keep  the rows to draw, or null for all of them
 * @return the rows to draw and what each of their cells stands for
 */
export const combine = (table: ComparisonTable, keep: ReadonlySet<number> | null): CombinedTable => {
    const { first, second } = table
    return first && second && sameShape(first.source, second.source)
        ? aligned(first, second, keep)
        : stacked(first, second)
}

/**
 * Whether the two tables can be read row against row: as many rows, and as many cells in each of them.
 *
 * A row of one file that is wider than the row beside it holds cells the other has no place for, and
 * drawing the two as one would leave those cells out of the table altogether.
 */
const sameShape = (first: RawTableCell[][], second: RawTableCell[][]): boolean =>
    first.length === second.length && first.every((row, index) => row.length === second[index]?.length)

export default combine
