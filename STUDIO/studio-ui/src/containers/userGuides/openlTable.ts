import type { RawTableCell } from 'types/tables'
import { type CsvCell, type CsvRow, csvRecords } from './csvRecords'

/** The line ending the column headers. */
const SEPARATOR = '---'

/** An OpenL table as the table editor draws it. */
export interface OpenLTableLayout {
    /** The cells, row by row, the table header first; a cell another one spans is covered. */
    rows: RawTableCell[][]
    /** How many rows are headers: the table header, then the column headers above the `---` line. */
    headerRows: number
}

const isSeparator = (row: CsvRow): boolean =>
    row.cells.length === 1 && !row.cells[0]?.quoted && row.cells[0]?.value === SEPARATOR

const isMarker = (cell: CsvCell | undefined, marker: string): boolean => cell?.quoted === false && cell.value === marker

/** The address the table editor gives a cell: its column letters, then its row number, as Excel writes it. */
const addressOf = (row: number, column: number): string => {
    let letters = ''
    for (let rest = column + 1; rest > 0; rest = Math.floor((rest - 1) / 26)) {
        letters = String.fromCodePoint(65 + ((rest - 1) % 26)) + letters
    }
    return `${letters}${row + 1}`
}

/** How many cells to the right, then below, the cell at the given place spans. */
const spanOf = (grid: CsvCell[][], row: number, column: number): [number, number] => {
    let width = 1
    while (isMarker(grid[row]?.[column + width], '<')) {
        width++
    }
    let height = 1
    while (isMarker(grid[row + height]?.[column], '^')) {
        height++
    }
    return [width, height]
}

/**
 * Lays out the OpenL table an `openl` code block draws.
 *
 * The first line is the header of the table, taken as it is written, commas included, and it spans the whole width.
 * The other lines are CSV records, padded to the widest one; a `---` line ends the column headers. A cell `<` joins
 * the cell on its left and a cell `^` the cell above. A marker that joins nothing a rectangle allows is drawn as the
 * text it is.
 */
export const openlTableOf = (text: string): OpenLTableLayout => {
    const lines = text.split('\n')
    const header = lines.findIndex(line => line.trim() !== '')
    if (header < 0) {
        return { rows: [], headerRows: 0 }
    }
    const records = csvRecords(lines.map((line, i) => (i === header ? '' : line)).join('\n'))
    const separator = records.findIndex(isSeparator)
    const body = records.filter(row => !isSeparator(row)).map(row => row.cells)
    const width = Math.max(1, ...body.map(cells => cells.length))
    const pad = (cells: CsvCell[]): CsvCell[] =>
        [...cells, ...Array.from({ length: width - cells.length }, () => ({ value: '', quoted: true }))]
    const title = { value: lines[header]?.trim() ?? '', quoted: true }
    const spanned = Array.from({ length: width - 1 }, () => ({ value: '<', quoted: false }))
    const grid: CsvCell[][] = [[title, ...spanned], ...body.map(pad)]
    const covered = grid.map(cells => cells.map(() => false))
    const rows = grid.map((cells, r) => cells.map((cell, c): RawTableCell => {
        if (covered[r]?.[c] === true) {
            return { covered: true }
        }
        const [spanWidth, spanHeight] = isMarker(cell, '<') || isMarker(cell, '^') ? [1, 1] : spanOf(grid, r, c)
        covered.slice(r, r + spanHeight).forEach((coveredRow, dr) => {
            coveredRow.fill(true, dr === 0 ? c + 1 : c, c + spanWidth)
        })
        return {
            cell: addressOf(r, c),
            value: cell.value,
            ...(spanWidth > 1 ? { colspan: spanWidth } : {}),
            ...(spanHeight > 1 ? { rowspan: spanHeight } : {}),
        }
    }))
    return { rows, headerRows: separator < 0 ? 1 : 1 + separator }
}
