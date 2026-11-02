import type { RawTableCell } from 'types/tables'
import type { ComparisonLine, ComparisonSide, ComparisonTable } from 'types/compare'

/** One of the two files of a comparison. */
export type FileSide = 'first' | 'second'

/** The rows and the columns of a table both files hold, lined up. */
export interface Alignment {
    rows: ComparisonLine[]
    columns: ComparisonLine[]
}

/** A cell merged with no other. */
const withoutSpans = ({ rowspan: _rows, colspan: _columns, ...rest }: RawTableCell): RawTableCell => rest

const widthOf = (source: RawTableCell[][]): number => Math.max(0, ...source.map(row => row.length))

/** Tells whether a row of a side holds a cell the side marks as read differently. */
const changedRowsOf = (side: ComparisonSide): ((row: number) => boolean) => {
    const changed = new Set(side.changedCells ?? [])
    return row => !!side.source[row]?.some(cell => cell.cell && changed.has(cell.cell))
}

/**
 * The lines of two files paired in their order.
 *
 * The longer file holds the lines the shorter one does not. Those lines are added or removed.
 */
const inOrder = (count: number, otherCount: number, changed: (line: number) => boolean): ComparisonLine[] =>
    Array.from({ length: Math.max(count, otherCount) }, (_, line): ComparisonLine => {
        if (line >= count) {
            return { second: line, status: 'added' }
        }
        if (line >= otherCount) {
            return { first: line, status: 'removed' }
        }
        return { first: line, second: line, status: changed(line) ? 'changed' : 'equal' }
    })

/** The rows of two files paired in their order: a pair is changed when either row holds a changed cell. */
const rowsInOrder = (first: ComparisonSide, second: ComparisonSide): ComparisonLine[] => {
    const firstChanged = changedRowsOf(first)
    const secondChanged = changedRowsOf(second)
    return inOrder(first.source.length, second.source.length, row => firstChanged(row) || secondChanged(row))
}

/**
 * The rows and the columns of a table both files hold, as the comparison lined them up.
 *
 * Lines the comparison did not line up are paired in their order.
 *
 * @return the alignment, or null for a table only one file holds
 */
export const alignmentOf = (table: ComparisonTable): Alignment | null => {
    const { first, second } = table
    if (!first || !second) {
        return null
    }
    const rows = table.rows?.length ? table.rows : rowsInOrder(first, second)
    const columns = table.columns?.length
        ? table.columns
        : inOrder(widthOf(first.source), widthOf(second.source), () => false)
    return { rows, columns }
}

/** The place of each line of a file among the lines lined up, by the line of the file. */
const placesOf = (lines: ComparisonLine[], side: FileSide): Map<number, number> => {
    const places = new Map<number, number>()
    lines.forEach((line, place) => {
        const own = line[side]
        if (own != null) {
            places.set(own, place)
        }
    })
    return places
}

/**
 * One file of a table laid out over the lines both files hold.
 *
 * A line only the other file holds is an empty line here. That keeps every line against its pair in the
 * other file. A merge stretches over the empty lines inside it, so it keeps the cells it covers.
 *
 * @return the cells by the lines lined up, a row and a column of every line
 */
export const alignSide = (source: RawTableCell[][], alignment: Alignment, side: FileSide): RawTableCell[][] => {
    const rowAt = placesOf(alignment.rows, side)
    const columnAt = placesOf(alignment.columns, side)
    const grid = alignment.rows.map(() => alignment.columns.map((): RawTableCell => ({})))
    source.forEach((cells, row) => cells.forEach((cell, column) => {
        const top = rowAt.get(row)
        const left = columnAt.get(column)
        if (cell.covered || top === undefined || left === undefined) {
            return
        }
        const bottom = rowAt.get(row + (cell.rowspan ?? 1) - 1) ?? top
        const right = columnAt.get(column + (cell.colspan ?? 1) - 1) ?? left
        for (let y = top; y <= bottom; y++) {
            for (let x = left; x <= right; x++) {
                grid[y]![x] = { covered: true }
            }
        }
        grid[top]![left] = {
            ...withoutSpans(cell),
            ...(bottom > top ? { rowspan: bottom - top + 1 } : {}),
            ...(right > left ? { colspan: right - left + 1 } : {}),
        }
    }))
    return grid
}

/** The status of a line that only the given file holds. */
export const ownStatusOf = (side: FileSide): ComparisonLine['status'] => (side === 'first' ? 'removed' : 'added')

/**
 * The merges of a table made whole again.
 *
 * A merge that would cover a cell it does not hold is undone. A cell covered by no merge shows as an empty cell.
 */
export const wholeMerges = (rows: RawTableCell[][]): RawTableCell[][] => {
    const owned = rows.map(row => row.map(() => false))
    const fits = (top: number, left: number, cell: RawTableCell): boolean => {
        for (let y = top; y < top + (cell.rowspan ?? 1); y++) {
            for (let x = left; x < left + (cell.colspan ?? 1); x++) {
                const inside = y !== top || x !== left
                if (inside && (!rows[y]?.[x]?.covered || owned[y]![x])) {
                    return false
                }
            }
        }
        return true
    }
    const result = rows.map(row => [...row])
    rows.forEach((row, y) => row.forEach((cell, x) => {
        if (cell.covered) {
            return
        }
        if (!fits(y, x, cell)) {
            result[y]![x] = withoutSpans(cell)
            return
        }
        for (let yy = y; yy < y + (cell.rowspan ?? 1); yy++) {
            for (let xx = x; xx < x + (cell.colspan ?? 1); xx++) {
                owned[yy]![xx] = true
            }
        }
    }))
    return result.map((row, y) => row.map((cell, x) => (cell.covered && !owned[y]![x] ? {} : cell)))
}
