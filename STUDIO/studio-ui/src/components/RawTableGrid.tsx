import React from 'react'
import { Tooltip } from 'antd'
import type { RawTableCell, TableLayout } from 'types/tables'
import { RawTableCellText, type OpenUsage } from './RawTableCellText'
import { type RawTableGridStyles, useStyles } from './RawTableGrid.styles'

/** How the screen showing a table marks one of its cells. */
export interface CellDecoration {
    /** Class the cell is drawn with, on top of the table's own. */
    className?: string | undefined
    /** The screen paints the cell itself, so its Excel background is left out from under that paint. */
    painted?: boolean
    /** The cell is beside the point on this screen, so its colours are drawn in grey. */
    muted?: boolean
    /** What the cell shows, when the screen has more to say about it than the workbook does. */
    content?: React.ReactNode
}

interface RawTableGridProps {
    /** The table body as the Tables API reports it, indexed rows[row][column]. */
    rows: RawTableCell[][]
    /** How each cell is marked; a cell the screen says nothing about is drawn as the workbook has it. */
    decorate?: ((cell: RawTableCell, row: number, column: number) => CellDecoration | undefined) | undefined
    /** Draw the formula a cell was written with rather than the value it computed, where it has one. */
    formulas?: boolean | undefined
    /** Follows a piece of a cell's text to the table it names; absent when this screen cannot go there. */
    onOpenUsage?: OpenUsage | undefined
    /** Told which cell the reader picked; absent on a screen where a cell cannot be picked. */
    onPickCell?: ((row: number, column: number) => void) | undefined
    /** Told which cell the reader opened, by double-clicking it. */
    onOpenCell?: ((row: number, column: number) => void) | undefined
    /**
     * What the keyboard does with the table: moving between cells, opening one, writing into one.
     *
     * <p>Given only where the table can be written. The table takes the focus so that the keys reach it and
     * nothing else — a screen full of other fields keeps its own.
     */
    onKeyDown?: ((event: React.KeyboardEvent<HTMLTableElement>) => void) | undefined
    /** The table itself, so the screen can hand it the focus once a cell is picked. */
    tableRef?: React.Ref<HTMLTableElement> | undefined
    /**
     * How the table is laid out, where the screen numbers the lines of its data — the cases of a test table.
     * Absent where the lines are not numbered, which is every other table.
     */
    layout?: TableLayout | undefined
    testId?: string | undefined
}

const formatValue = (value: RawTableCell['value']): string => (value == null ? '' : String(value))

/** Stable row key from the first cell's A1 address (e.g. `A2`); falls back to the row position. */
const rowKey = (row: RawTableCell[], index: number): string => {
    const address = row.find(cell => cell.cell)?.cell
    return address ?? `r${index}`
}

/** How much of its brightness a muted colour keeps. */
const MUTED_BRIGHTNESS = 0.8

const HEX_COLOUR = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i

/**
 * The grey a colour reads as when its cell is beside the point: the brightness of the colour itself,
 * dimmed, so that what the cell is filled with still tells light from dark.
 *
 * A colour written in any other way is left alone, and so is a colour the cell does not carry - an
 * unfilled cell stays unfilled rather than turning grey.
 */
const mute = (colour: string | undefined): string | undefined => {
    if (!colour || !HEX_COLOUR.test(colour)) {
        return colour
    }
    const digits = colour.length === 4
        ? [...colour.slice(1)].map(digit => digit + digit).join('')
        : colour.slice(1)
    const value = Number.parseInt(digits, 16)
    const average = (((value >> 16) & 0xff) + ((value >> 8) & 0xff) + (value & 0xff)) / 3
    const grey = Math.round(average * MUTED_BRIGHTNESS)
    return `rgb(${grey}, ${grey}, ${grey})`
}

/**
 * The cell's Excel styling. A cell the screen paints keeps its font and alignment but not its own
 * background, which would otherwise sit over the paint. A muted cell keeps everything but the colours,
 * which are drawn in grey.
 */
const cellStyle = (style: RawTableCell['style'], painted: boolean, muted: boolean): React.CSSProperties => ({
    background: painted ? undefined : (muted ? mute(style?.background) : style?.background),
    color: muted ? mute(style?.color) : style?.color,
    textAlign: style?.align as React.CSSProperties['textAlign'],
    verticalAlign: style?.valign as React.CSSProperties['verticalAlign'],
    fontWeight: style?.bold ? 'bold' : undefined,
    fontStyle: style?.italic ? 'italic' : undefined,
    textDecoration: style?.underline ? 'underline' : undefined,
})

/**
 * Draws a table the way its author wrote it in Excel: the same cells, the same merges, the same
 * styling, with the values already evaluated — or, where the screen asks for it, with the formulas the
 * cells were written with, which every cell carries beside its value.
 *
 * Every screen that shows a table of a workbook — the trace window, the comparison — draws it through
 * this component and only says how its own cells are marked, so a table looks the same everywhere.
 */
/**
 * The text of one cell, marked with what the compiler knows about it.
 *
 * What it knows describes the value the cell holds, and the ranges it marks are measured over that text. A
 * cell shown as the formula it was written with is another text altogether, so it is drawn plain — which is
 * what the legacy editor did, where the formula replaced the marked content.
 */
const cellText = (cell: RawTableCell, formulas: boolean, styles: RawTableGridStyles,
    onOpenUsage?: OpenUsage) => {
    const asFormula = formulas && Boolean(cell.formula)
    const text = formatValue(asFormula ? cell.formula : cell.value)
    const metaInfo = asFormula ? undefined : cell.metaInfo
    // Most cells have nothing marked — what the compiler knows about them is the type behind them and the
    // editor they ask for. Those are drawn as the text they are, rather than through a component of their own.
    if (!metaInfo?.usages?.length && !metaInfo?.returnCell) {
        return text
    }
    return <RawTableCellText metaInfo={metaInfo} onOpenUsage={onOpenUsage} styles={styles} text={text} />
}

export const RawTableGrid: React.FC<RawTableGridProps> = ({
    rows,
    decorate,
    formulas,
    onOpenUsage,
    onPickCell,
    onOpenCell,
    onKeyDown,
    tableRef,
    layout,
    testId,
}) => {
    const { styles, cx } = useStyles()
    // How many lines of data there are, and so how many numbers: from where the data begins to the end of the
    // table, counted down the rows or across the columns according to how the table is written.
    // The grid is as wide as its widest row; a covered cell takes a place of its own in the matrix, so the
    // count is the column count the browser lays the table out in.
    const columns = rows.reduce((widest, row) => Math.max(widest, row.length), 0)
    // How many lines of data there are, and so how many numbers: from where the data begins to the end of the
    // table, counted down the rows or across the columns according to how the table is written.
    const lines = layout === undefined
        ? 0
        : Math.max(0, (layout.transposed ? columns : rows.length) - layout.firstDataLine)

    return (
        <table
            ref={tableRef}
            className={styles.table}
            data-testid={testId}
            onKeyDown={onKeyDown}
            tabIndex={onKeyDown === undefined ? undefined : -1}
        >
            <tbody>
                {/*
                  * A transposed table's data runs across its columns, so its numbers run above them — one cell
                  * per column of the grid, blank over the headings the data begins after.
                  */}
                {layout?.transposed && lines > 0 && (
                    <tr>
                        {Array.from({ length: columns }, (unused, column) => (
                            <td className={styles.lineNumber} key={column}>
                                {column >= layout.firstDataLine
                                    ? <span data-testid="table-line-number">{column - layout.firstDataLine + 1}</span>
                                    : null}
                            </td>
                        ))}
                    </tr>
                )}
                {rows.map((row, rowIndex) => (
                    <tr key={rowKey(row, rowIndex)}>
                        {/* A table written the usual way round is numbered down its side, as the Editor did. */}
                        {layout !== undefined && !layout.transposed && (
                            <td className={styles.lineNumber}>
                                {rowIndex >= layout.firstDataLine
                                    ? <span data-testid="table-line-number">{rowIndex - layout.firstDataLine + 1}</span>
                                    : null}
                            </td>
                        )}
                        {row.map((cell, columnIndex) => {
                            if (cell.covered) return null
                            const decoration = decorate?.(cell, rowIndex, columnIndex)
                            const key = cell.cell ?? `c${columnIndex}`
                            const drawn = (
                                <td
                                    colSpan={cell.colspan}
                                    data-cell={cell.cell}
                                    onClick={onPickCell && (() => onPickCell(rowIndex, columnIndex))}
                                    onDoubleClick={onOpenCell && (() => onOpenCell(rowIndex, columnIndex))}
                                    rowSpan={cell.rowspan}
                                    style={cellStyle(cell.style, !!decoration?.painted, !!decoration?.muted)}
                                    className={cx(styles.cell, cell.comment !== undefined && styles.commented,
                                        decoration?.className)}
                                >
                                    {decoration?.content ?? cellText(cell, !!formulas, styles, onOpenUsage)}
                                </td>
                            )
                            // The note is shown while the cell is read. A cell the screen has taken over — one
                            // being written into — shows what the screen put there, not a note over the top of it.
                            return cell.comment === undefined || decoration?.content !== undefined
                                ? <React.Fragment key={key}>{drawn}</React.Fragment>
                                : (
                                    <Tooltip
                                        key={key}
                                        placement="rightBottom"
                                        title={<span className={styles.note}>{cell.comment}</span>}
                                    >
                                        {drawn}
                                    </Tooltip>
                                )
                        })}
                    </tr>
                ))}
            </tbody>
        </table>
    )
}

export default RawTableGrid
