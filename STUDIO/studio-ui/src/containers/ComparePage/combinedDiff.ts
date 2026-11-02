import type { RawTableCell } from 'types/tables'
import type { ComparisonLine, ComparisonLineStatus, ComparisonSide, ComparisonTable } from 'types/compare'
import { alignmentOf, alignSide, wholeMerges } from './alignment'
import { keepRows } from './diffRows'

/** What the combined view says about one cell of the table. */
export interface CellMark {
    /** What became of the row of the cell. */
    row: ComparisonLineStatus
    /** The file the cell is of alone, when its row or its column only one file holds. */
    paint?: 'added' | 'removed'
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

/**
 * The cell leading a row of the combined view. It carries the row's own address, because the rows of
 * the two files meet in one table and the address a cell has in its own file is not unique there.
 */
const leadOf = (into: CombinedTable): RawTableCell => ({ cell: `#${into.rows.length}` })

/** Adds a row led by the sign of what became of it. */
const addRow = (into: CombinedTable, kind: ComparisonLineStatus, cells: RawTableCell[], marks: CellMark[]): void => {
    const lead = leadOf(into)
    into.marks.set(lead, { row: kind, lead: true })
    cells.forEach((cell, column) => into.marks.set(cell, marks[column]!))
    into.rows.push([lead, ...cells])
}

/** The rows of a table only one file holds, each led by the sign the whole table is read by. */
const oneSided = (side: ComparisonSide | undefined, kind: 'added' | 'removed', into: CombinedTable): void => {
    side?.source.forEach(row => addRow(into, kind, row, row.map(() => ({ row: kind, paint: kind }))))
}

/** The file a line is of alone, if any. */
const ownerOf = (line: ComparisonLine): 'added' | 'removed' | undefined =>
    line.status === 'added' || line.status === 'removed' ? line.status : undefined

/**
 * The cell the combined view shows where a row and a column meet, and what it says about it.
 *
 * A removed row or a removed column shows the cell of the first file. A merge of the second file covering it wins.
 * Everything else shows the cell of the second file.
 */
const pick = (
    row: ComparisonLine,
    column: ComparisonLine,
    was: RawTableCell,
    now: RawTableCell,
    changed: ReadonlySet<string>
): { cell: RawTableCell; mark: CellMark } => {
    const fromFirst = row.status === 'removed' || (row.status !== 'added' && column.status === 'removed' && !now.covered)
    const cell = fromFirst ? was : now
    const paint = ownerOf(row) ?? ownerOf(column)
    if (!paint && !!now.cell && changed.has(now.cell)) {
        return { cell, mark: { row: row.status, before: text(was), after: text(now) } }
    }
    return { cell, mark: paint ? { row: row.status, paint } : { row: row.status } }
}

/**
 * The two versions of one table drawn as a single one.
 *
 * Both files are lined up row by row and column by column, as the comparison found them. A row or a column only
 * one file holds stands where it stands in that file. Where the two files read a cell differently, the cell
 * carries both values. Every row is led by a sign saying what became of it.
 *
 * A table only one file holds is shown whole, as dropped or as added.
 *
 * @param table the table as the comparison reports it
 * @param keep  the rows lined up to draw, or null for all of them
 * @return the rows to draw and what each of their cells stands for
 */
export const combine = (table: ComparisonTable, keep: ReadonlySet<number> | null): CombinedTable => {
    const combined: CombinedTable = { rows: [], marks: new Map() }
    const alignment = alignmentOf(table)
    if (!alignment || !table.first || !table.second) {
        oneSided(table.first, 'removed', combined)
        oneSided(table.second, 'added', combined)
        return combined
    }
    const changed = new Set(table.second.changedCells ?? [])
    const rows = alignment.rows.filter((_, index) => !keep || keep.has(index))
    const was = alignSide(table.first.source, alignment, 'first')
    const now = alignSide(table.second.source, alignment, 'second')
    const wasKept = keep ? keepRows(was, keep) : was
    const nowKept = keep ? keepRows(now, keep) : now
    const picked = rows.map((row, y) => alignment.columns.map((column, x) =>
        pick(row, column, wasKept[y]![x]!, nowKept[y]![x]!, changed)))
    const cells = wholeMerges(picked.map(row => row.map(({ cell }) => cell)))
    cells.forEach((row, y) => addRow(combined, rows[y]!.status, row, picked[y]!.map(({ mark }) => mark)))
    return combined
}

export default combine
