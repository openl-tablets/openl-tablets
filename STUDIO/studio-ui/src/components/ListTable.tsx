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
     * line, and every line is drawn. Its columns stand still as the lines go by: a column widens only for a wider value
     * or name, the others sharing what is left of the box.
     */
    columnsInSight?: boolean | undefined
    /**
     * Lays the list out the other way round: every column is a line under its name, and every row is a column. The
     * first column heads the rows, its name in the corner. A name keeps to one line up to a width of its own, and a
     * longer one wraps, within a word too.
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

/** A column of a transposed list with the cell of its head, which is as wide as the column. */
type HeadColumn = readonly [HTMLElement, Element]

/** The columns of a transposed list with the cells of its head, the names first. */
const headColumns = (head: HTMLTableRowElement, columnSet: HTMLTableColElement | null): HeadColumn[] => {
    const cells = [...head.cells]
    return ([...columnSet?.children ?? []] as HTMLElement[]).flatMap((col, at): HeadColumn[] => {
        const cell = cells[at]
        return cell === undefined ? [] : [[col, cell]]
    })
}

/** How wide a cell is drawn, unrounded. */
const widthOf = (cell: Element): number => cell.getBoundingClientRect().width

/**
 * The widths the columns of a transposed list are held at, read off the cells of its head.
 *
 * A column is held at the very width it is drawn at, not rounded: rounded up, the columns held took more than the box,
 * and every column held widened a little every frame. A column widens only once it is drawn wider than it holds its
 * cell, past a fraction of a pixel.
 *
 * The names never narrow. A column of a row that a value takes wider than it is held — a value no narrower column can
 * hold — keeps the width its value needed, even once the value is out of sight. When the names widen or a row outgrows
 * its column, the other columns of the rows share anew what the box leaves them, and narrow for it.
 */
class HeldWidths {
    /** How wide each column held holds its cell. */
    private readonly held = new WeakMap<HTMLElement, number>()
    /** The width a column of a row was taken to by its values once it was held. */
    private readonly needed = new WeakMap<HTMLElement, number>()
    /** The widest each cell has been drawn at since the columns were last held, by its place. */
    private drawn: number[] = []
    private boxWidth: number

    constructor(private readonly box: HTMLElement) {
        this.boxWidth = box.clientWidth
    }

    /** Reads how wide the cells are drawn, to be held: the widest since the columns were last held. */
    read(columns: readonly HeadColumn[]): void {
        columns.forEach(([, cell], at) => {
            this.drawn[at] = Math.max(this.drawn[at] ?? 0, widthOf(cell))
        })
    }

    /**
     * Holds the columns at the widths read since they were last held. A box of another width, a scroll bar coming or
     * going included, holds them anew from the cells as they are drawn in it.
     */
    hold(columns: readonly HeadColumn[]): void {
        const drawn = this.drawn
        this.drawn = []
        if (this.box.clientWidth !== this.boxWidth) {
            this.boxWidth = this.box.clientWidth
            this.forget(columns)
            this.holdAsDrawn(columns)
            return
        }
        const [names, ...rows] = columns
        const [namesWidth, ...rowWidths] = drawn
        const namesGrew = names !== undefined && namesWidth !== undefined && this.outgrows(names[0], namesWidth)
        let grew = namesGrew
        rows.forEach(([col], at) => {
            const width = rowWidths[at]
            if (width === undefined || !this.outgrows(col, width)) {
                return
            }
            grew = true
            // A column held before and drawn wider since was taken so wide by its values.
            if (this.held.has(col)) {
                this.needed.set(col, width)
            }
        })
        if (!grew) {
            return
        }
        if (namesGrew) {
            names[0].style.width = `${namesWidth}px`
        }
        // The rows share anew what is left, each at least as wide as its values needed.
        rows.forEach(([col]) => {
            const width = this.needed.get(col)
            col.style.width = width === undefined ? '' : `${width}px`
        })
        this.holdAsDrawn(rows)
        if (namesGrew) {
            this.held.set(names[0], widthOf(names[1]))
        }
    }

    /** Lets every column go, as it is drawn by its cells alone. */
    forget(columns: readonly HeadColumn[]): void {
        columns.forEach(([col]) => {
            col.style.width = ''
            this.needed.delete(col)
        })
    }

    private outgrows(col: HTMLElement, width: number): boolean {
        return width > (this.held.get(col) ?? 0) + 0.5
    }

    /** Holds the columns at the widths their cells are drawn at now. */
    private holdAsDrawn(columns: readonly HeadColumn[]): void {
        // Every width is read before any is written, so the list is laid out once for all of them.
        const widths = columns.map(([, cell]) => widthOf(cell))
        columns.forEach(([col], at) => {
            const width = widths[at] ?? 0
            col.style.width = `${width}px`
            this.held.set(col, width)
        })
    }
}

/**
 * Holds the columns of a transposed list at the widths they have been drawn at, read off the head of the list.
 *
 * The browser lays a table out by the lines drawn, and which lines are drawn follows how tall they are. A line whose
 * name widens the first column wraps the values beside it and makes every line taller. The line then leaves the lines
 * drawn, the column narrows again, and the line comes back: the list would be drawn anew on every frame. Held, the
 * columns stand still as the lines go by, and a column widens only for a wider value, the others sharing what is left
 * of the box, as far as the values let them.
 *
 * A column does not narrow when what it holds shrinks, as a value folded up. The widths start anew when the box
 * changes its width, and for other lines or rows: another page of the rows, other values, names or language.
 *
 * @param scroller the box the lines scroll in
 * @param kept     whether the widths are kept at all
 * @param lines    the lines the widths are kept for
 * @param rows     the keys of the rows the widths are kept for
 * @return the row of the head of the list, whose cells are as wide as the columns, and the columns, the names first
 */
const useKeptWidths = (scroller: HTMLDivElement | null, kept: boolean, lines: unknown, rows: string) => {
    const headRow = React.useRef<HTMLTableRowElement>(null)
    const columnSet = React.useRef<HTMLTableColElement>(null)
    React.useLayoutEffect(() => {
        const row = headRow.current
        if (!kept || scroller === null || row === null) {
            return undefined
        }
        // The same cells and columns while the lines and the rows stay the same.
        const columns = headColumns(row, columnSet.current)
        const widths = new HeldWidths(scroller)
        widths.forget(columns)
        // The widths are read as the browser tells the cells resized, while the lines that widened them are still
        // drawn, and held before the next frame is laid out: held at once, they would resize the very cells the
        // browser is telling of, which it reports as an error.
        let frame: number | null = null
        const observer = new ResizeObserver(() => {
            widths.read(columns)
            frame ??= requestAnimationFrame(() => {
                frame = null
                widths.hold(columns)
            })
        })
        observer.observe(scroller)
        columns.forEach(([, cell]) => observer.observe(cell))
        return () => {
            observer.disconnect()
            if (frame !== null) {
                cancelAnimationFrame(frame)
            }
        }
    }, [kept, lines, rows, scroller])
    return { headRow, columnSet }
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
    // The widths of the columns are kept for these lines and these rows, where only the lines in sight are drawn.
    const widthsKept = head !== undefined && measure !== undefined
    const { headRow, columnSet } = useKeptWidths(scroller, widthsKept, lines, JSON.stringify(rows.map(rowKey)))

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
            {widthsKept && (
                <colgroup ref={columnSet} data-testid="list-column-widths">
                    <col />
                    {rows.map(row => <col key={rowKey(row)} />)}
                </colgroup>
            )}
            {head && (
                <thead className={cx(shared.listHead, maxHeight && styles.stickyHead)}>
                    <tr ref={headRow}>
                        <th className={nameClass}><span className={styles.lineNameText}>{head.title}</span></th>
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
                            <span className={styles.lineNameText}>{column.title}</span>
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
