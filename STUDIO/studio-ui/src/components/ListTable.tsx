import React from 'react'
import { Spin } from 'antd'
import { useVirtualizer } from '@tanstack/react-virtual'
import { useLaidOut } from 'hooks/useLaidOut'
import { windowRect } from 'hooks/useScrollMargin'
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
    /** A click in the cell is the cell's own: it reads or opens what the cell holds, and does not pick the row. */
    ownClicks?: boolean
}

interface ListTableProps<T> {
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
    /**
     * Draws the columns in sight and a few beside them, and the others as the list is scrolled across to them: a
     * list of thousands of columns — the results of a test table comparing thousands of values — draws a screenful.
     * A column is as wide as its header, measured as it is drawn. Where the list has no layout to measure, every
     * column is drawn.
     */
    columnsInSight?: boolean | undefined
}

const keepClick = (event: React.MouseEvent) => event.stopPropagation()

/** How wide a column not drawn yet is taken to be, in pixels. */
const COLUMN_ESTIMATE = 120

/** How many columns beyond those in sight are drawn, on either side. */
const COLUMNS_AHEAD = 3

/** The room of the columns not drawn, on either side of those that are: no padding, only the width they take. */
const roomOf = (width: number): React.CSSProperties => ({ width, minWidth: width, padding: 0 })

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
    columnsInSight,
    ...rest
}: ListTableProps<T>): React.ReactElement => {
    const { styles: shared } = useListPageStyles()
    const { styles, cx } = useStyles()
    // The element the list scrolls across in, kept as state so the list is drawn again once it is there to measure.
    const [scroller, setScroller] = React.useState<HTMLDivElement | null>(null)
    const laidOut = useLaidOut(scroller, 'width')
    const virtual = !!columnsInSight && laidOut
    // Kept while the columns stay the same, so a render that only scrolls does not measure them all anew.
    const getItemKey = React.useCallback((index: number) => columns[index]?.key ?? index, [columns])
    const virtualizer = useVirtualizer<HTMLDivElement, HTMLTableCellElement>({
        count: columns.length,
        enabled: virtual,
        estimateSize: () => COLUMN_ESTIMATE,
        getItemKey,
        getScrollElement: () => scroller,
        horizontal: true,
        // Before the list is there to measure, it is taken to be as wide as the window: the first drawing already
        // holds the columns in sight rather than none, and the list does not change its height once measured.
        initialRect: windowRect(),
        overscan: COLUMNS_AHEAD,
        // The columns come into sight with the next frame rather than inside the scroll itself.
        useFlushSync: false,
    })
    const items = virtualizer.getVirtualItems()
    // The columns drawn, by their place in the list, and the room of those that are not, before and after them.
    const drawn: Array<[number, ListTableColumn<T>]> = virtual
        ? items.flatMap(item => {
            const column = columns[item.index]
            return column === undefined ? [] : [[item.index, column]]
        })
        : columns.map((column, index) => [index, column])
    const before = virtual ? (items[0]?.start ?? 0) : 0
    const after = virtual ? Math.max(0, virtualizer.getTotalSize() - (items.at(-1)?.end ?? 0)) : 0

    const cellClass = (column: ListTableColumn<T>) => cx(
        column.fit && styles.fit,
        column.align === 'center' && styles.center,
        column.align === 'right' && styles.right
    )

    return (
        <Spin spinning={loading ?? false}>
            {/* Only a list drawing the columns in sight measures what it scrolls in. */}
            <div
                ref={columnsInSight ? setScroller : undefined}
                className={styles.scroll}
                style={maxHeight ? { maxHeight } : undefined}
            >
                <table className={cx(shared.listTable, styles.table)} data-testid={rest['data-testid']}>
                    <thead className={cx(shared.listHead, shared.microLabel, maxHeight && styles.stickyHead)}>
                        <tr>
                            {before > 0 && <th data-testid="list-columns-before" style={roomOf(before)} />}
                            {drawn.map(([index, column]) => (
                                <th
                                    key={column.key}
                                    ref={virtual ? virtualizer.measureElement : undefined}
                                    className={cellClass(column)}
                                    data-index={index}
                                >
                                    {column.title}
                                </th>
                            ))}
                            {after > 0 && <th data-testid="list-columns-after" style={roomOf(after)} />}
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
                                {before > 0 && <td data-testid="list-cells-before" style={roomOf(before)} />}
                                {drawn.map(([, column]) => (
                                    <td
                                        key={column.key}
                                        className={cellClass(column)}
                                        onClick={column.ownClicks ? keepClick : undefined}
                                    >
                                        {column.render(row)}
                                    </td>
                                ))}
                                {after > 0 && <td data-testid="list-cells-after" style={roomOf(after)} />}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </Spin>
    )
}

export default ListTable
