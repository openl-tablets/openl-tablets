import React from 'react'
import { Spin } from 'antd'
import { useListPageStyles } from 'styles/listPageStyles'
import { onActivate } from 'utils/keyboardActivate'
import { useStyles } from './ListTable.styles'

/** One column of a list: the caption it stands under and what a row puts in the cell. */
export interface ListTableColumn<T> {
    key: string
    title: React.ReactNode
    render: (row: T) => React.ReactNode
    /** The column is only as wide as it needs to be. */
    fit?: boolean
    align?: 'center' | 'right'
}

export interface ListTableProps<T> {
    columns: ListTableColumn<T>[]
    rows: T[]
    rowKey: (row: T) => string
    'data-testid'?: string
    /** Whether a row is picked, so that it is marked as such. */
    selected?: ((row: T) => boolean) | undefined
    /** Picks the row that was clicked. A row without it is read, not chosen. */
    onRowClick?: ((row: T) => void) | undefined
    /** Scrolls the rows inside up to this height, keeping the header in view. */
    maxHeight?: string | undefined
    /** Whether the rows are still on their way. */
    loading?: boolean | undefined
}

/**
 * A list of rows under a header, the way every list screen of OpenL Studio shows one.
 *
 * The look is the shared one: a framed table, small capitals for the column names, a line under every row and
 * a tint under the one the pointer is on. A screen only says what its columns are and what a row puts in them.
 *
 * A list that is asked to scroll keeps its header in view above the rows.
 */
export const ListTable = <T, >({
    columns,
    rows,
    rowKey,
    selected,
    onRowClick,
    maxHeight,
    loading,
    ...rest
}: ListTableProps<T>): React.ReactElement => {
    const { styles: shared } = useListPageStyles()
    const { styles, cx } = useStyles()

    const cellClass = (column: ListTableColumn<T>) => cx(
        column.fit && styles.fit,
        column.align === 'center' && styles.center,
        column.align === 'right' && styles.right
    )

    return (
        <Spin spinning={loading ?? false}>
            <div className={styles.scroll} style={maxHeight ? { maxHeight } : undefined}>
                <table className={cx(shared.listTable, styles.table)} data-testid={rest['data-testid']}>
                    <thead className={cx(shared.listHead, shared.microLabel, maxHeight && styles.stickyHead)}>
                        <tr>
                            {columns.map(column => (
                                <th key={column.key} className={cellClass(column)}>{column.title}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map(row => (
                            <tr
                                key={rowKey(row)}
                                onClick={onRowClick && (() => onRowClick(row))}
                                onKeyDown={onRowClick && onActivate(() => onRowClick(row))}
                                // A row that is chosen is reached by the keyboard as well, so it takes focus.
                                tabIndex={onRowClick ? 0 : undefined}
                                className={cx(
                                    shared.listRow,
                                    onRowClick ? styles.clickable : styles.noHover,
                                    selected?.(row) && styles.selectedRow
                                )}
                            >
                                {columns.map(column => (
                                    <td key={column.key} className={cellClass(column)}>{column.render(row)}</td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </Spin>
    )
}

export default ListTable
