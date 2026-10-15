import type {
    RawTableCell,
    RawTableCellData,
    RawTableCellStyle,
    RawTableTextRun,
    RawTableTextRunData,
    RawTableView,
    RawTableViewData,
} from 'types/tables'

/** The text the pieces of a cell cut up: its value as the workbook writes it. */
const textOf = (value: RawTableCellData['value']): string => (value == null ? '' : String(value))

/** The pieces of a cell text, each with the text it covers and its font. */
const runsOf = (
    cell: RawTableCellData,
    runs: RawTableTextRunData[],
    styles: RawTableCellStyle[] | undefined
): RawTableTextRun[] => {
    const text = textOf(cell.value)
    let from = 0
    return runs.map(run => {
        const piece: RawTableTextRun = { text: text.slice(from, run.to) }
        const style = run.style === undefined ? undefined : styles?.[run.style]
        if (style !== undefined) {
            piece.style = style
        }
        from = run.to
        return piece
    })
}

/**
 * A cell as a screen reads it: its style and the fonts of its pieces taken from the styles of the read. Cells of one
 * style share one style object, so a large table holds each style once in memory as well.
 */
const expandCell = (cell: RawTableCellData, styles: RawTableCellStyle[] | undefined): RawTableCell => {
    const { style, runs, ...rest } = cell
    const expanded: RawTableCell = rest
    const own = style === undefined ? undefined : styles?.[style]
    if (own !== undefined) {
        expanded.style = own
    }
    if (runs !== undefined) {
        expanded.runs = runsOf(cell, runs, styles)
    }
    return expanded
}

/**
 * The cells of a table as a screen reads them, from the cells the server sends: each style and each font named by
 * index is taken from the styles of the read.
 *
 * @param source the cells as the server sends them, row by row
 * @param styles the styles they name, or undefined for a read of the values alone
 * @returns the cells, row by row
 */
export const expandCells = (source: RawTableCellData[][], styles: RawTableCellStyle[] | undefined): RawTableCell[][] =>
    source.map(row => row.map(cell => expandCell(cell, styles)))

/**
 * A raw table as a screen reads it, from the table the server sends ({@link expandCells}).
 *
 * @param view the table as the server sends it
 * @returns the table, every cell carrying its own style
 */
export const expandRawTable = (view: RawTableViewData): RawTableView => {
    const { source, styles, ...rest } = view
    return { ...rest, source: expandCells(source, styles) }
}
