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
     * Draws the columns in sight and a few beside them, and the others as the list is scrolled to them: a list of
     * thousands of columns — the results of a test table comparing thousands of values — draws a screenful. A column
     * is as wide as its header, or as tall as its line in a transposed list, measured as it is drawn. Where the list
     * has no layout to measure, every column is drawn.
     *
     * A transposed list scrolls its lines inside the box of its `maxHeight`: without one, the box is as tall as every
     * line, and every line is drawn.
     */
    columnsInSight?: boolean | undefined
    /**
     * Lays the list out the other way round: every column is a line under its name, and every row is a column. The
     * first column heads the rows, its name in the corner.
     *
     * A transposed list is read, not chosen: a row is neither picked nor marked as picked. A list asked to scroll keeps
     * its head in view above the lines, and the names of the lines beside the rows.
     */
    transposed?: boolean | undefined
}

const keepClick = (event: React.MouseEvent) => event.stopPropagation()

/** How wide a column not drawn yet is taken to be, in pixels. */
const COLUMN_ESTIMATE = 120

/** How tall a line of a transposed list not drawn yet is taken to be, in pixels. */
const LINE_ESTIMATE = 40

/** How many columns beyond those in sight are drawn, on either side. */
const COLUMNS_AHEAD = 3

/** The room of the columns not drawn, on either side of those that are: no padding, only the width they take. */
const roomOf = (width: number): React.CSSProperties => ({ width, minWidth: width, padding: 0 })

/** The columns of a list that are drawn, by their place in it, and the room of the others before and after them. */
interface ColumnsDrawn<T> {
    drawn: Array<[number, ListTableColumn<T>]>
    before: number
    after: number
    /** Measures a column as it is drawn, where only the columns in sight are drawn. */
    measure: ((element: Element | null) => void) | undefined
}

/**
 * The columns of a list to draw: the columns in sight, when the list is asked to draw only those and is laid out to
 * measure. Every column otherwise.
 *
 * The columns go across the list, or down it where the list is transposed.
 */
const useColumnsDrawn = <T, >(
    columns: ListTableColumn<T>[],
    scroller: HTMLDivElement | null,
    transposed: boolean,
    inSight: boolean
): ColumnsDrawn<T> => {
    const laidOut = useLaidOut(scroller, transposed ? 'height' : 'width')
    const virtual = inSight && laidOut
    // Kept while the columns stay the same, so a render that only scrolls does not measure them all anew.
    const getItemKey = React.useCallback((index: number) => columns[index]?.key ?? index, [columns])
    const virtualizer = useVirtualizer<HTMLDivElement, Element>({
        count: columns.length,
        enabled: virtual,
        estimateSize: () => (transposed ? LINE_ESTIMATE : COLUMN_ESTIMATE),
        getItemKey,
        getScrollElement: () => scroller,
        horizontal: !transposed,
        // Before the list is there to measure, it is taken to be as large as the window: the first drawing already
        // holds the columns in sight rather than none, and the list does not change its size once measured.
        initialRect: windowRect(),
        overscan: COLUMNS_AHEAD,
        // The columns come into sight with the next frame rather than inside the scroll itself.
        useFlushSync: false,
    })
    if (!virtual) {
        return { drawn: columns.map((column, index) => [index, column]), before: 0, after: 0, measure: undefined }
    }
    const items = virtualizer.getVirtualItems()
    return {
        drawn: items.flatMap(item => {
            const column = columns[item.index]
            return column === undefined ? [] : [[item.index, column]]
        }),
        before: items[0]?.start ?? 0,
        after: Math.max(0, virtualizer.getTotalSize() - (items.at(-1)?.end ?? 0)),
        measure: virtualizer.measureElement,
    }
}

type ListStyles = ReturnType<typeof useStyles>

/** The class of a cell of a column: its alignment and, where the column has a width of its own, its width. */
const cellClassOf = <T, >({ styles, cx }: ListStyles, column: ListTableColumn<T>, withWidth: boolean) => cx(
    withWidth && column.fit && styles.fit,
    column.align === 'center' && styles.center,
    column.align === 'right' && styles.right
)

/**
 * What every list is drawn in: the spinner while its rows are on their way, the box it scrolls in, and the table.
 * Only a list drawing what is in sight is handed the box, to measure what it scrolls in.
 *
 * It reads no styles of its own: the list hands it the classes it has read already.
 */
const ListFrame: React.FC<{
    scrollClass: string
    tableClass: string
    loading: boolean | undefined
    maxHeight: string | undefined
    scrollsIn: ((element: HTMLDivElement | null) => void) | undefined
    /** The box takes the focus, so the keyboard scrolls it where a browser does not let it reach the box otherwise. */
    focusable?: boolean
    'data-testid': string | undefined
    children: React.ReactNode
}> = ({ scrollClass, tableClass, loading, maxHeight, scrollsIn, focusable, children, ...rest }) => (
    <Spin spinning={loading ?? false}>
        <div
            ref={scrollsIn}
            className={scrollClass}
            style={maxHeight ? { maxHeight } : undefined}
            tabIndex={focusable ? 0 : undefined}
        >
            <table className={tableClass} data-testid={rest['data-testid']}>
                {children}
            </table>
        </div>
    </Spin>
)

/** A row of a list: a line under the names of the columns. */
const RowsList = <T, >({
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
    const look = useStyles()
    const { styles, cx } = look
    // The element the list scrolls across in, kept as state so the list is drawn again once it is there to measure.
    const [scroller, setScroller] = React.useState<HTMLDivElement | null>(null)
    const { drawn, before, after, measure } = useColumnsDrawn(columns, scroller, false, !!columnsInSight)
    // The class of each column drawn, put together once rather than for every cell of it.
    const classes = drawn.map(([, column]) => cellClassOf(look, column, true))

    return (
        <ListFrame
            data-testid={rest['data-testid']}
            loading={loading}
            maxHeight={maxHeight}
            scrollClass={styles.scroll}
            scrollsIn={columnsInSight ? setScroller : undefined}
            tableClass={cx(shared.listTable, styles.table)}
        >
            <thead className={cx(shared.listHead, shared.microLabel, maxHeight && styles.stickyHead)}>
                <tr>
                    {before > 0 && <th data-testid="list-columns-before" style={roomOf(before)} />}
                    {drawn.map(([index, column], place) => (
                        <th key={column.key} ref={measure} className={classes[place]} data-index={index}>
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
                        {drawn.map(([, column], place) => (
                            <td
                                key={column.key}
                                className={classes[place]}
                                onClick={column.ownClicks ? keepClick : undefined}
                            >
                                {column.render(row)}
                            </td>
                        ))}
                        {after > 0 && <td data-testid="list-cells-after" style={roomOf(after)} />}
                    </tr>
                ))}
            </tbody>
        </ListFrame>
    )
}

/** A column of a list to a line, under its name, the rows across under the first column. */
const ColumnsList = <T, >({
    columns,
    rows,
    rowKey,
    maxHeight,
    loading,
    columnsInSight,
    ...rest
}: ListTableProps<T>): React.ReactElement => {
    const { styles: shared } = useListPageStyles()
    const look = useStyles()
    const { styles, cx } = look
    // The element the list scrolls down in, kept as state so the list is drawn again once it is there to measure.
    const [scroller, setScroller] = React.useState<HTMLDivElement | null>(null)
    const head = columns[0]
    // Kept while the columns stay the same, so a render that only scrolls does not measure the lines anew.
    const lines = React.useMemo(() => columns.slice(1), [columns])
    const { drawn, before, after, measure } = useColumnsDrawn(lines, scroller, true, !!columnsInSight)

    // The classes of a line, of its name and of the cells of each column, put together once, not for every cell.
    const lineClass = cx(shared.listRow, styles.noHover, styles.line)
    const nameClass = cx(shared.microLabel, styles.lineName)
    const headClass = head && cellClassOf(look, head, false)
    const classes = drawn.map(([, column]) => cellClassOf(look, column, false))

    // The room of the lines not drawn, above and below those that are.
    const room = (height: number, testId: string) => (
        <tr data-testid={testId} style={{ height }}>
            <td colSpan={rows.length + 1} style={{ padding: 0 }} />
        </tr>
    )

    return (
        <ListFrame
            data-testid={rest['data-testid']}
            // The lines scroll inside the box, out of reach of the keys that scroll the page.
            focusable={!!maxHeight}
            loading={loading}
            maxHeight={maxHeight}
            scrollClass={styles.scroll}
            scrollsIn={columnsInSight ? setScroller : undefined}
            tableClass={cx(shared.listTable, styles.table)}
        >
            {head && (
                <thead className={cx(shared.listHead, maxHeight && styles.stickyHead)}>
                    <tr>
                        <th className={nameClass}>{head.title}</th>
                        {rows.map(row => (
                            <th
                                key={rowKey(row)}
                                className={headClass}
                                onClick={head.ownClicks ? keepClick : undefined}
                            >
                                {head.render(row)}
                            </th>
                        ))}
                    </tr>
                </thead>
            )}
            <tbody>
                {before > 0 && room(before, 'list-lines-before')}
                {drawn.map(([index, column], place) => (
                    <tr key={column.key} className={lineClass}>
                        {/* A line is measured by its name: a browser may measure the height of a table row
                            without its rules. */}
                        <th ref={measure} className={nameClass} data-index={index}>
                            {column.title}
                        </th>
                        {rows.map(row => (
                            <td
                                key={rowKey(row)}
                                className={classes[place]}
                                onClick={column.ownClicks ? keepClick : undefined}
                            >
                                {column.render(row)}
                            </td>
                        ))}
                    </tr>
                ))}
                {after > 0 && room(after, 'list-lines-after')}
            </tbody>
        </ListFrame>
    )
}

/**
 * A list of rows under a header, the way every list screen of OpenL Studio shows one.
 *
 * The look is the shared one: a framed table, small capitals for the column names, a line under every row and
 * a tint under the one the pointer is on. A screen only says what its columns are and what a row puts in them.
 *
 * A list that is asked to scroll keeps its header in view above the rows. A transposed list shows every column as a
 * line instead, the rows across.
 */
export const ListTable = <T, >(props: ListTableProps<T>): React.ReactElement => (props.transposed
    ? <ColumnsList {...props} />
    : <RowsList {...props} />)

export default ListTable
