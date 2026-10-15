import type { RawTableCell, RawTableCellBorderSide, RawTableCellStyle } from 'types/tables'
import { wider } from '../../components/rawTableStyle'

/**
 * The rows of a table with the first ones kept out of sight, such as the header of a table shown without it.
 *
 * A line two cells share may be named by either of them, in the style the workbook holds and in the look of a table
 * theme alike, and the grid draws the wider one. So the line under the rows kept out of sight is drawn on the top of
 * the first row left, where it is wider than a line that row draws of its own. The line under a hidden header stays.
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
 * A cell drawn with the line under the cell above it on its top, where that line is wider than the line the cell
 * draws there itself. A cell a merged region covers is drawn by that region.
 */
const underLineOf = (cell: RawTableCell, above: RawTableCell | undefined): RawTableCell => {
    if (above === undefined || cell.covered) {
        return cell
    }
    const own = cell.style?.border?.top
    const line = wider(own, above.style?.border?.bottom)
    return line === undefined || line === own ? cell : { ...cell, style: topped(cell.style, line) }
}

/** A style with a line on its top. */
const topped = (style: RawTableCellStyle | undefined, line: RawTableCellBorderSide): RawTableCellStyle =>
    ({ ...style, border: { ...style?.border, top: line } })
