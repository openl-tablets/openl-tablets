import type { RawTableCell, RawTableCellBorderSide, RawTableCellStyle } from 'types/tables'

/**
 * The rows of a table with the first ones kept out of sight, such as the header of a table shown without it.
 *
 * A line the table theme draws between two cells is read on the upper cell: the grid draws the side of the upper cell
 * over the side of the cell under it. So the line the theme draws under the rows kept out of sight is drawn on the top
 * of the first row left, where that row draws no line of its own. The line under a hidden header stays.
 *
 * @param rows the rows of the table
 * @param hidden how many rows at the top are kept out of sight
 * @returns the rows left, the given ones when no row is kept out of sight
 */
export const withoutFirstRows = (rows: RawTableCell[][], hidden: number): RawTableCell[][] => {
    if (hidden === 0) {
        return rows
    }
    const left = rows.slice(hidden)
    const first = left[0]
    if (first !== undefined) {
        left[0] = first.map((cell, column) => underLineOf(cell, holderOf(rows, hidden - 1, column)))
    }
    return left
}

/**
 * The cell of the table whose region holds a place, or undefined where no cell read holds it.
 *
 * A place a merged region covers is held by the cell the region starts at, above it or on its left.
 */
const holderOf = (rows: RawTableCell[][], row: number, column: number): RawTableCell | undefined => {
    for (let r = row; r >= 0; r--) {
        for (let c = column; c >= 0; c--) {
            const cell = rows[r]?.[c]
            if (cell !== undefined && !cell.covered
                && r + (cell.rowspan ?? 1) > row && c + (cell.colspan ?? 1) > column) {
                return cell
            }
        }
    }
    return undefined
}

/**
 * A cell drawn with the line the theme draws under the cell above it on its top. A cell a merged region covers is
 * drawn by that region, and a style drawing a line on its top keeps that line.
 */
const underLineOf = (cell: RawTableCell, above: RawTableCell | undefined): RawTableCell => {
    const line = above?.style?.source === 'theme' ? above.style.border?.bottom : undefined
    if (cell.covered || cell.style?.source !== 'theme' || !takes(cell.style, line)) {
        return cell
    }
    return { ...cell, style: topped(cell.style, line) }
}

/** Whether a style takes a line on its top: there is a line, and the style draws none there of its own. */
const takes = (style: RawTableCellStyle | undefined, line: RawTableCellBorderSide | undefined):
    line is RawTableCellBorderSide => line !== undefined && style?.border?.top === undefined

/** A style with a line on its top. */
const topped = (style: RawTableCellStyle | undefined, line: RawTableCellBorderSide): RawTableCellStyle =>
    ({ ...style, border: { ...style?.border, top: line } })
