import React, { useMemo } from 'react'
import { RawTableGrid } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import { csvRecords } from './csvRecords'
import { openlTableOf } from './openlTable'
import { useStyles } from './UserGuides.styles'

interface TableBlockProps {
    /** The text of the code block. */
    text: string
}

/** A `csv` code block, drawn as a table whose first record is the column header. */
export const CsvTable: React.FC<TableBlockProps> = ({ text }) => {
    const { styles } = useStyles()
    const [header = [], ...rows] = useMemo(() => csvRecords(text).map(row => row.cells.map(cell => cell.value)), [text])
    const width = Math.max(header.length, ...rows.map(cells => cells.length))
    const padded = (cells: string[]) => [...cells, ...Array.from({ length: width - cells.length }, () => '')]

    return (
        <div className={styles.tableScroll}>
            <table className={styles.table} data-testid="guide-csv-table">
                <thead>
                    <tr>{padded(header).map((value, column) => <th key={column}>{value}</th>)}</tr>
                </thead>
                <tbody>
                    {rows.map((cells, row) => (
                        <tr key={row}>{padded(cells).map((value, column) => <td key={column}>{value}</td>)}</tr>
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
