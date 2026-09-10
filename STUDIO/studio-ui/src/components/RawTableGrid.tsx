import React from 'react'
import type { RawTableCell } from 'types/tables'
import { useStyles } from './RawTableGrid.styles'

/** How the screen showing a table marks one of its cells. */
export interface CellDecoration {
    /** Class the cell is drawn with, on top of the table's own. */
    className?: string | undefined
    /** The screen paints the cell itself, so its Excel background is left out from under that paint. */
    painted?: boolean
}

interface RawTableGridProps {
    /** The table body as the Tables API reports it, indexed rows[row][column]. */
    rows: RawTableCell[][]
    /** How each cell is marked; a cell the screen says nothing about is drawn as the workbook has it. */
    decorate?: (cell: RawTableCell) => CellDecoration | undefined
    testId?: string
}

const formatValue = (value: RawTableCell['value']): string => (value == null ? '' : String(value))

/** Stable row key from the first cell's A1 address (e.g. `A2`); falls back to the row position. */
const rowKey = (row: RawTableCell[], index: number): string => {
    const address = row.find(cell => cell.cell)?.cell
    return address ?? `r${index}`
}

/**
 * The cell's Excel styling. A cell the screen paints keeps its font and alignment but not its own
 * background, which would otherwise sit over the paint.
 */
const cellStyle = (style: RawTableCell['style'], painted: boolean): React.CSSProperties => ({
    background: painted ? undefined : style?.background,
    color: style?.color,
    textAlign: style?.align as React.CSSProperties['textAlign'],
    verticalAlign: style?.valign as React.CSSProperties['verticalAlign'],
    fontWeight: style?.bold ? 'bold' : undefined,
    fontStyle: style?.italic ? 'italic' : undefined,
    textDecoration: style?.underline ? 'underline' : undefined,
})

/**
 * Draws a table the way its author wrote it in Excel: the same cells, the same merges, the same
 * styling, with the values already evaluated.
 *
 * Every screen that shows a table of a workbook — the trace window, the comparison — draws it through
 * this component and only says how its own cells are marked, so a table looks the same everywhere.
 */
export const RawTableGrid: React.FC<RawTableGridProps> = ({ rows, decorate, testId }) => {
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
                                    style={cellStyle(cell.style, !!decoration?.painted)}
                                >
                                    {formatValue(cell.value)}
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
