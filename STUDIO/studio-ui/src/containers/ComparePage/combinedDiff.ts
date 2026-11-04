import type { RawTableCell } from 'types/tables'
import type {
    ComparisonLine,
    ComparisonLineStatus,
    ComparisonSide,
    ComparisonTable,
    ConflictComparisonLine,
    ConflictComparisonSide,
    ConflictComparisonTable,
} from 'types/compare'
import { alignmentOf, alignSide, wholeMerges } from './alignment'
import { keepRows, type ShownLine } from './diffRows'

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
    /**
     * The value of their version, when both versions of a conflict changed the cell differently: the base is
     * `before`, your version `after`.
     */
    theirs?: string
    /** The cell is of a line one version of a conflict removed and the other one changed. */
    conflict?: boolean
    /** The cell leads its row and carries the sign the row is read by rather than a value. */
    lead?: boolean
    /** The cell stands for this many equal rows left out, across the whole table. */
    gap?: number
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

/** Adds a row standing for the equal rows left out, one cell across the signs and the table. */
const addGap = (into: CombinedTable, rows: number, width: number): void => {
    const gap: RawTableCell = { colspan: width + 1 }
    into.marks.set(gap, { row: 'equal', gap: rows })
    into.rows.push([gap, ...Array.from({ length: width }, (): RawTableCell => ({ covered: true }))])
}

/** A cell picked for the combined view, with what it says about itself. */
interface PickedCell {
    cell: RawTableCell
    mark: CellMark
}

/**
 * Adds the rows picked to draw, each led by the sign of what became of it, with the rows standing for the equal rows
 * left out between them. The merges are made whole over the rows drawn; none of them crosses a gap.
 *
 * @param lines   the rows to draw and the gaps between them
 * @param drawn   what became of each row drawn, in order
 * @param picked  the cells of each row drawn, in order
 * @param width   how many columns the table has
 */
const addLines = (
    into: CombinedTable,
    lines: readonly ShownLine[],
    drawn: readonly ComparisonLineStatus[],
    picked: readonly PickedCell[][],
    width: number
): void => {
    const cells = wholeMerges(picked.map(row => row.map(({ cell }) => cell)))
    let next = 0
    lines.forEach(line => {
        if ('gap' in line) {
            addGap(into, line.gap, width)
            return
        }
        addRow(into, drawn[next]!, cells[next]!, picked[next]!.map(({ mark }) => mark))
        next++
    })
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
): PickedCell => {
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
 * A table only one file holds is shown whole, as dropped or as added. Equal rows left out are one row that says how
 * many they are.
 *
 * @param table the table as the comparison reports it
 * @param shown the rows lined up to draw and the gaps between them, or null for all of them
 * @return the rows to draw and what each of their cells stands for
 */
export const combine = (table: ComparisonTable, shown: readonly ShownLine[] | null): CombinedTable => {
    const combined: CombinedTable = { rows: [], marks: new Map() }
    const alignment = alignmentOf(table)
    if (!alignment || !table.first || !table.second) {
        oneSided(table.first, 'removed', combined)
        oneSided(table.second, 'added', combined)
        return combined
    }
    const changed = new Set(table.second.changedCells ?? [])
    const width = alignment.columns.length
    const lines = shown ?? alignment.rows.map((_, line): ShownLine => ({ line }))
    const was = keepRows(alignSide(table.first.source, alignment, 'first'), lines, width).rows
    const now = keepRows(alignSide(table.second.source, alignment, 'second'), lines, width).rows
    const drawn = lines.flatMap((line, y) => ('line' in line ? [{ row: alignment.rows[line.line]!, y }] : []))
    const picked = drawn.map(({ row, y }) => alignment.columns.map((column, x) =>
        pick(row, column, was[y]![x]!, now[y]![x]!, changed)))
    addLines(combined, lines, drawn.map(({ row }) => row.status), picked, width)
    return combined
}

/** What a version of a conflict did to a line: inserted it, removed it, changed it or left it as it was. */
const lineStatusOf = (line: ConflictComparisonLine): ComparisonLineStatus => {
    const statuses = new Set([line.yoursStatus, line.theirsStatus])
    return (['removed', 'added', 'changed'] as const).find(status => statuses.has(status)) ?? 'equal'
}

/** The colour of a cell on a line a version removed or inserted: removed wins, as the base shows the cell. */
const paintOf = (row: ComparisonLineStatus, column: ComparisonLineStatus): 'added' | 'removed' | undefined => {
    if (row === 'removed' || column === 'removed') {
        return 'removed'
    }
    return row === 'added' || column === 'added' ? 'added' : undefined
}

/** Whether one version of a conflict removed a line the other one changed. */
const removedAgainstChange = (line: ConflictComparisonLine): boolean =>
    (line.yoursStatus === 'removed' && line.theirsStatus === 'changed')
    || (line.theirsStatus === 'removed' && line.yoursStatus === 'changed')

/** The cell of a version where a row and a column meet, when the version holds both. */
const cellAt = (
    side: ConflictComparisonSide | undefined,
    row: number | null | undefined,
    column: number | null | undefined
): RawTableCell | undefined => (side && row != null && column != null ? side.source[row]?.[column] : undefined)

/**
 * The two versions of a table of a conflict drawn as a single one, each compared with the base they were made from.
 *
 * The rows and the columns stand as the base lines them up. A line a version inserted is painted as added, and one
 * a version removed as removed. A cell one version changed carries the base value and the value of that version. A
 * cell both versions changed differently carries the base value and the values of both, your version first: that is
 * a conflict. Every row is led by the sign of what became of it. Equal rows left out are one row that says how many
 * they are.
 *
 * @param table the table of the conflict as the comparison reports it
 * @param shown the rows to draw and the gaps between them, or null for all of them
 */
export const combineConflict = (table: ConflictComparisonTable, shown: readonly ShownLine[] | null): CombinedTable => {
    const combined: CombinedTable = { rows: [], marks: new Map() }
    const rows = table.rows ?? []
    const columns = table.columns ?? []
    const { base, yours, theirs } = table
    if (rows.length === 0 || columns.length === 0) {
        // A table only some versions hold: what the versions added is added, each of them, and what only the base holds
        // is removed.
        if (base) {
            oneSided(!yours || !theirs ? base : undefined, 'removed', combined)
        } else {
            oneSided(yours, 'added', combined)
            oneSided(theirs, 'added', combined)
        }
        return combined
    }
    const changedByYours = new Set(yours?.changedCells ?? [])
    const changedByTheirs = new Set(theirs?.changedCells ?? [])
    const conflicts = new Set(yours?.conflictCells ?? [])
    const cellOf = (row: ConflictComparisonLine, column: ConflictComparisonLine): PickedCell => {
        const was = cellAt(base, row.base, column.base)
        const mine = cellAt(yours, row.yours, column.yours)
        const other = cellAt(theirs, row.theirs, column.theirs)
        const status = lineStatusOf(row)
        const paint = paintOf(status, lineStatusOf(column))
        if (paint) {
            const cell = (paint === 'removed' ? was ?? mine ?? other : mine ?? other ?? was) ?? {}
            // A line one version removed while the other one changed it is a conflict too.
            const conflict = paint === 'removed' && (removedAgainstChange(row) || removedAgainstChange(column))
            return { cell, mark: { row: status, paint, ...(conflict && { conflict }) } }
        }
        if (mine?.cell && conflicts.has(mine.cell)) {
            return { cell: mine, mark: { row: status, before: text(was), after: text(mine), theirs: text(other) } }
        }
        if (mine?.cell && changedByYours.has(mine.cell)) {
            return { cell: mine, mark: { row: status, before: text(was), after: text(mine) } }
        }
        if (other?.cell && changedByTheirs.has(other.cell)) {
            return { cell: other, mark: { row: status, before: text(was), after: text(other) } }
        }
        return { cell: mine ?? other ?? was ?? {}, mark: { row: status } }
    }
    const lines = shown ?? rows.map((_, line): ShownLine => ({ line }))
    const drawn = lines.flatMap(line => ('line' in line ? [rows[line.line]!] : []))
    const picked = drawn.map(row => columns.map(column => cellOf(row, column)))
    addLines(combined, lines, drawn.map(lineStatusOf), picked, columns.length)
    return combined
}

export default combine
