import React from 'react'
import type { RawTableCell } from 'types/tables'
import { RawTableCellText, type OpenUsage } from './RawTableCellText'
import { useStyles } from './RawTableGrid.styles'

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
    decorate?: (cell: RawTableCell) => CellDecoration | undefined
    /** Draw the formula a cell was written with rather than the value it computed, where it has one. */
    formulas?: boolean
    /** Follows a piece of a cell's text to the table it names; absent when this screen cannot go there. */
    onOpenUsage?: OpenUsage | undefined
    testId?: string
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
const cellText = (cell: RawTableCell, formulas: boolean, onOpenUsage?: OpenUsage) => {
    const asFormula = formulas && Boolean(cell.formula)
    const text = formatValue(asFormula ? cell.formula : cell.value)
    const metaInfo = asFormula ? undefined : cell.metaInfo
    // Most cells have nothing marked — what the compiler knows about them is the type behind them and the
    // editor they ask for. Those are drawn as the text they are, rather than through a component of their own.
    if (!metaInfo?.usages?.length && !metaInfo?.returnCell) {
        return text
    }
    return <RawTableCellText metaInfo={metaInfo} onOpenUsage={onOpenUsage} text={text} />
}

export const RawTableGrid: React.FC<RawTableGridProps> = ({ rows, decorate, formulas, onOpenUsage, testId }) => {
    const { styles, cx } = useStyles()

    return (
        <table className={styles.table} data-testid={testId}>
            <tbody>
                {rows.map((row, rowIndex) => (
                    <tr key={rowKey(row, rowIndex)}>
                        {row.map((cell, columnIndex) => {
                            if (cell.covered) return null
                            const decoration = decorate?.(cell)
                            return (
                                <td
                                    key={cell.cell ?? `c${columnIndex}`}
                                    className={cx(styles.cell, decoration?.className)}
                                    colSpan={cell.colspan}
                                    data-cell={cell.cell}
                                    rowSpan={cell.rowspan}
                                    style={cellStyle(cell.style, !!decoration?.painted, !!decoration?.muted)}
                                >
                                    {decoration?.content ?? cellText(cell, !!formulas, onOpenUsage)}
                                </td>
                            )
                        })}
                    </tr>
                ))}
            </tbody>
        </table>
    )
}

export default RawTableGrid
