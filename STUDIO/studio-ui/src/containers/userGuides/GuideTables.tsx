import React, { useMemo } from 'react'
import { RawTableGrid } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import { type CsvRow, csvRecords } from './csvRecords'
import { openlTableOf } from './openlTable'
import { useStyles } from './UserGuides.styles'

interface TableBlockProps {
    /** The text of the code block. */
    text: string
}

/** A `csv` code block, drawn as a table whose first record is the column header. */
export const CsvTable: React.FC<TableBlockProps> = ({ text }) => {
    const { styles } = useStyles()
    const [header, ...rows] = useMemo(() => csvRecords(text), [text])
    // The table is as wide as its longest record; a shorter record leaves its last cells blank.
    const width = Math.max(header?.cells.length ?? 0, ...rows.map(row => row.cells.length))
    const columns = Array.from({ length: width }, (unused, column) => column)
    const valueAt = (row: CsvRow | undefined, column: number) => row?.cells[column]?.value ?? ''

    return (
        <div className={styles.tableScroll}>
            <table className={styles.table} data-testid="guide-csv-table">
                <thead>
                    <tr>{columns.map(column => <th key={column}>{valueAt(header, column)}</th>)}</tr>
                </thead>
                <tbody>
                    {/* A record starts on a line of its own, so its line tells it apart. */}
                    {rows.map(row => (
                        <tr key={row.line}>{columns.map(column => <td key={column}>{valueAt(row, column)}</td>)}</tr>
                    ))}
                </tbody>
            </table>
        </div>
    )
}

/** An `openl` code block, drawn the way the table editor of OpenL Studio draws an OpenL table. */
export const OpenLTable: React.FC<TableBlockProps> = ({ text }) => {
    const { styles } = useStyles()
    const { rows, headerRows } = useMemo(() => openlTableOf(text), [text])
    const decorate = useMemo(() => {
        const header = { className: styles.openlHeader }
        return (cell: RawTableCell, row: number) => (row < headerRows ? header : undefined)
    }, [headerRows, styles.openlHeader])

    return (
        <div className={styles.tableScroll}>
            <RawTableGrid decorate={decorate} rows={rows} testId="guide-openl-table" />
        </div>
    )
}
