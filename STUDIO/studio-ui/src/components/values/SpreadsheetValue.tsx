import React, { useMemo } from 'react'
import { Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import type { SpreadsheetResultView } from 'types/execution'
import { ValueCell } from './ParameterValues'

const { Text } = Typography

/** One row of the grid: the name of the step and the value of every column of that step. */
interface SpreadsheetRow {
    key: string
    step: string
    cells: unknown[]
}

export interface SpreadsheetValueProps {
    spreadsheet: SpreadsheetResultView
    /** Prefix of the value keys, unique on the screen. */
    keyPrefix: string
}

/**
 * A calculated spreadsheet, shown as the table its author wrote.
 *
 * Every step of the spreadsheet is a row and every column of it a column, so the result reads the way the
 * source table does. A cell holding a value with inner structure expands it field by field, the way the trace
 * shows the value of a cell.
 */
export const SpreadsheetValue: React.FC<SpreadsheetValueProps> = ({ spreadsheet, keyPrefix }) => {
    const { t } = useTranslation('execution')

    const columns: ListTableColumn<SpreadsheetRow>[] = useMemo(() => [
        {
            key: 'step',
            fit: true,
            title: t('run.step'),
            render: (row: SpreadsheetRow) => <Text strong>{row.step}</Text>,
        },
        ...spreadsheet.columns.map((column, index): ListTableColumn<SpreadsheetRow> => ({
            key: `column-${index}`,
            title: column ?? '',
            render: (row: SpreadsheetRow) => (
                <ValueCell path={`${keyPrefix}-${row.key}-${index}`} value={row.cells[index]} />
            ),
        })),
    ], [spreadsheet.columns, keyPrefix, t])

    const rows: SpreadsheetRow[] = useMemo(
        () => spreadsheet.rows.map((row, index) => ({
            key: `row-${index}`,
            step: row ?? '',
            cells: spreadsheet.cells[index] ?? [],
        })),
        [spreadsheet.rows, spreadsheet.cells]
    )

    return (
        <ListTable<SpreadsheetRow>
            columns={columns}
            data-testid={`spreadsheet-${keyPrefix}`}
            rowKey={row => row.key}
            rows={rows}
        />
    )
}

export default SpreadsheetValue
