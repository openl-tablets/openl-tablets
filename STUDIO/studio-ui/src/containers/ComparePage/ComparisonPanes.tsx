import React from 'react'
import { Badge, Empty, Flex, Spin, Splitter, Tag, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { RawTableGrid, type CellDecoration } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import type { ComparisonLine, ComparisonLineStatus, ComparisonSide, ComparisonTable } from 'types/compare'
import { alignmentOf, alignSide, ownStatusOf, type Alignment, type FileSide } from './alignment'
import { combine, type CellMark, type CombinedTable } from './combinedDiff'
import { keepRows, rowsToShow, type ShownLine } from './diffRows'
import { framesOf, type FrameSides } from './frames'
import { useStyles } from './ComparePage.styles'
import { sidesOf, useCellStyles } from './ComparisonPanes.styles'

/** Which of the two ways of reading a comparison is shown. */
export type DiffView = 'sides' | 'combined'

interface ComparisonPanesProps {
    /** The table the user picked, or null while nothing is picked. */
    table: ComparisonTable | null
    loading: boolean
    /** Whether the rows that read the same in both files are shown. */
    showEqualRows: boolean
    /** Whether the two versions stand side by side, as they do unless the reader says otherwise. */
    view?: DiffView
    error: string | null
    /** Put before the name of the first file; carries the controls the hidden list of elements left behind. */
    leading?: React.ReactNode
    /** Put at the end of the line the files are named on; the control that picks the view. */
    trailing?: React.ReactNode
    /** What the two sides are called, when they are not simply the first and the second file. */
    titles?: { first: string; second: string } | undefined
    /** Whether the second version stands on the left side by side, as your version does against the base. */
    secondOnLeft?: boolean | undefined
}

/** The sign a row of the combined view is read by. */
const SIGNS: Record<ComparisonLineStatus, string> = { equal: '', changed: '→', added: '+', removed: '−' }

/** The kinds of change a legend explains, in the order it explains them. */
const KINDS = ['changed', 'added', 'removed'] as const

type ChangeKind = typeof KINDS[number]

/** The colour of the dot a legend marks a kind of change with, matching the colour its cells are painted in. */
const DOTS: Record<ChangeKind, 'warning' | 'success' | 'error'> = {
    changed: 'warning',
    added: 'success',
    removed: 'error',
}

/** The kinds of change a table shows: changed cells, and lines or a whole table only one file holds. */
const kindsOf = (table: ComparisonTable, alignment: Alignment | null): ChangeKind[] => {
    if (!alignment) {
        return table.first ? ['removed'] : ['added']
    }
    const lines = [...alignment.rows, ...alignment.columns]
    const changed = (table.first?.changedCells?.length ?? 0) > 0
    return KINDS.filter(kind => (kind === 'changed' ? changed : lines.some(line => line.status === kind)))
}

/**
 * The two versions of the picked table, side by side or drawn as one table, with what differs marked.
 *
 * Side by side, each file heads its own column, on the line the controls of the comparison are on. The divider
 * between them gives either file more room. The rows of both files stand against each other. A row only one file
 * holds stands against an empty row in the other one, and so does a column. The first file stands on the left
 * unless the second one is asked to. A table only one file holds is shown on that side alone.
 *
 * Drawn as one, the two versions share a table. A cell the files read differently carries both values, and every
 * row is led by the sign it is read by.
 *
 * A legend above the table tells what each colour means. A table moved on its sheet says where from and where to. A
 * table whose formatting changed says so and is framed whole with a dashed line, as a merge frames it.
 */
export const ComparisonPanes: React.FC<ComparisonPanesProps> = ({
    table,
    loading,
    showEqualRows,
    view = 'sides',
    error,
    leading,
    trailing,
    titles,
    secondOnLeft = false,
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    const alignment = table ? alignmentOf(table) : null
    const rows = rowsToShow(alignment, showEqualRows)

    /** What is shown instead of a table: the comparison is not ready, or there is nothing to show. */
    const insteadOfTable = (): React.ReactNode => {
        if (loading) {
            return <div className={styles.center}><Spin /></div>
        }
        if (error) {
            return <Empty description={error} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        if (!table) {
            return <Empty description={t('nothing_selected')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return null
    }

    const pane = (head: React.ReactNode, testId: string, body: React.ReactNode) => (
        <div className={styles.column}>
            <div className={styles.head}>{head}</div>
            <div className={styles.body} data-testid={testId}>{body}</div>
        </div>
    )

    // What is said about the table stands on a line of its own above the files, so the rows of both files stay
    // level with each other.
    const notes = table && !loading && !error && (
        <Notes alignment={alignment} combined={view === 'combined'} table={table} />
    )
    const withNotes = (panes: React.ReactNode) => (
        <div className={styles.column}>
            {notes}
            {panes}
        </div>
    )

    if (view === 'combined') {
        const head = (
            <>
                {leading}
                <span className={cx(styles.headLabel)}>
                    {`${titles?.first ?? t('file_first')} ${SIGNS.changed} ${titles?.second ?? t('file_second')}`}
                </span>
                {trailing && <span className={styles.headAction}>{trailing}</span>}
            </>
        )
        const body = insteadOfTable() ?? (
            <Restyled restyled={table!.formattingChanged}>
                <Combined combined={combine(table!, rows)} />
            </Restyled>
        )
        return withNotes(<div className={styles.panes}>{pane(head, 'compare-pane-combined', body)}</div>)
    }

    const content = (side: FileSide) => {
        const source = table?.[side]
        return insteadOfTable()
            ?? (source
                ? (
                    <Restyled restyled={table?.formattingChanged}>
                        <Side alignment={alignment} rows={rows} side={side} source={source} />
                    </Restyled>
                )
                : <Empty description={t('absent')} image={Empty.PRESENTED_IMAGE_SIMPLE} />)
    }

    // The pane of a file; the controls lead the left pane and end the right one, whichever file each pane shows.
    const sidePane = (side: FileSide, left: boolean) => pane(
        <>
            {left && leading}
            <span className={cx(styles.headLabel)}>{titles?.[side] ?? t(`file_${side}`)}</span>
            {!left && trailing && <span className={styles.headAction}>{trailing}</span>}
        </>,
        `compare-pane-${side}`,
        content(side))
    const [left, right] = secondOnLeft ? ['second', 'first'] as const : ['first', 'second'] as const
    return withNotes(
        <Splitter className={styles.panes}>
            <Splitter.Panel min="20%">{sidePane(left, true)}</Splitter.Panel>
            <Splitter.Panel min="20%">{sidePane(right, false)}</Splitter.Panel>
        </Splitter>
    )
}

/**
 * What is said about a table, on a line above the files.
 *
 * A legend tells what each colour means. A moved table says where from and where to. A table whose formatting
 * changed says so, since its cells are not marked for it.
 */
const Notes: React.FC<{ table: ComparisonTable; alignment: Alignment | null; combined: boolean }> = ({
    table,
    alignment,
    combined,
}) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const kinds = table.status === 'equal' || table.status === 'moved' ? [] : kindsOf(table, alignment)
    const moved = table.moved && table.firstRange && table.secondRange
    if (kinds.length === 0 && !moved && !table.formattingChanged) {
        return null
    }
    return (
        <Flex wrap align="center" className={styles.notes} data-testid="compare-notes" gap="small">
            {kinds.length > 0 && (
                <Flex data-testid="compare-legend" gap="small">
                    {kinds.map(kind => (
                        <Badge
                            key={kind}
                            status={DOTS[kind]}
                            text={combined ? `${SIGNS[kind]} ${t(`status_${kind}`)}` : t(`status_${kind}`)}
                        />
                    ))}
                </Flex>
            )}
            {table.formattingChanged && (
                <Tag color="warning" data-testid="compare-formatting-changed">{t('formatting_changed')}</Tag>
            )}
            {moved && (
                <Typography.Text data-testid="compare-moved" type="secondary">
                    {t('moved_places', { first: table.firstRange, second: table.secondRange })}
                </Typography.Text>
            )}
        </Flex>
    )
}

/** The class of a changed cell, framed along the sides its frame runs. */
const useFrame = () => {
    const { styles, cx } = useCellStyles()
    return (sides: FrameSides) => cx(styles.changedFrame, sidesOf(styles, sides))
}

/** The cell standing for the equal rows left out between the rows shown, saying how many they are. */
const useGap = () => {
    const { t } = useTranslation('compare')
    const { styles } = useCellStyles()
    return (rows: number): CellDecoration => ({
        className: styles.gap,
        painted: true,
        content: t('rows_hidden', { count: rows }),
    })
}

/** A version of a table, framed whole with a dashed line when its formatting changed, as a merge frames it. */
const Restyled: React.FC<{ restyled: boolean | undefined; children: React.ReactNode }> = ({ restyled, children }) => {
    const { styles } = useCellStyles()
    return <div className={restyled ? styles.restyled : undefined} data-testid="compare-restyled">{children}</div>
}

/**
 * One file of the table, its rows and its columns standing where they stand against the other file.
 *
 * A line only this file holds is painted as added or removed. A line only the other file holds is an empty line
 * here. The cells the other file reads differently are framed, and changed cells next to each other share one frame.
 * Every other cell keeps the colours the workbook gives it. Equal rows left out are one row that says how many they
 * are.
 */
const Side: React.FC<{
    source: ComparisonSide
    side: FileSide
    alignment: Alignment | null
    rows: ShownLine[] | null
}> = ({ source, side, alignment, rows }) => {
    const { styles } = useCellStyles()
    const frame = useFrame()
    const gapOf = useGap()
    const changed = new Set(source.changedCells ?? [])
    const own = ownStatusOf(side)
    const paint = own === 'added' ? styles.rowAdded : styles.rowRemoved

    if (!alignment) {
        // A table only this file holds is all of it added or removed.
        const decorate = (): CellDecoration => ({ className: paint, painted: true })
        return <RawTableGrid decorate={decorate} rows={source.source} />
    }

    const lines = rows ?? alignment.rows.map((_, line): ShownLine => ({ line }))
    const { rows: shown, gaps } = keepRows(alignSide(source.source, alignment, side), lines, alignment.columns.length)
    const shownRows = lines.map(line => ('line' in line ? alignment.rows[line.line] : undefined))
    // A changed cell stands on lines both files hold, so its address is enough to know it.
    const frames = framesOf(shown, cell => !!cell.cell && changed.has(cell.cell))
    const ownLine = (line: ComparisonLine | undefined) => line?.status === own

    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        const gap = gaps.get(cell)
        if (gap !== undefined) {
            return gapOf(gap)
        }
        const rowLine = shownRows[row]
        const columnLine = alignment.columns[column]
        if (rowLine?.[side] == null || columnLine?.[side] == null) {
            // A line only the other file holds has nothing here to mark.
            return undefined
        }
        if (ownLine(rowLine) || ownLine(columnLine)) {
            return { className: paint, painted: true }
        }
        const sides = frames.get(cell)
        return sides && { className: frame(sides) }
    }

    return <RawTableGrid decorate={decorate} rows={shown} />
}

/**
 * The two versions as one table: a cell the files read differently carries both values, and the sign
 * leading each row says what became of it.
 */
const Combined: React.FC<{ combined: CombinedTable }> = ({ combined }) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useCellStyles()
    const frame = useFrame()
    const gapOf = useGap()
    const { rows: source, marks } = combined
    const frames = framesOf(source, cell => marks.get(cell)?.before !== undefined)

    /** The colour a cell wholly of one file or the other is drawn in. */
    const ofSide = (kind: CellMark['paint']) => {
        if (kind === 'added') {
            return styles.rowAdded
        }
        return kind === 'removed' ? styles.rowRemoved : undefined
    }

    const decorate = (cell: RawTableCell): CellDecoration | undefined => {
        const mark = marks.get(cell)
        if (!mark) {
            return undefined
        }
        if (mark.gap !== undefined) {
            return gapOf(mark.gap)
        }
        if (mark.lead) {
            const row = mark.row === 'added' || mark.row === 'removed' ? mark.row : undefined
            return {
                className: cx(styles.combinedLead, ofSide(row)),
                painted: true,
                // The sign says what became of the row, and says so in words to whoever hovers it. A
                // row that reads the same in both files carries no sign, so there is nothing to hover.
                content: SIGNS[mark.row] && <span title={t(`status_${mark.row}`)}>{SIGNS[mark.row]}</span>,
            }
        }
        const sides = frames.get(cell)
        if (sides) {
            return {
                className: frame(sides),
                content: (
                    <>
                        <span className={styles.combinedBefore}>{mark.before}</span>
                        {` ${SIGNS.changed} `}
                        <span>{mark.after}</span>
                    </>
                ),
            }
        }
        // Only a cell wholly of one file or the other is painted over. Every other cell keeps what the workbook
        // fills it with.
        const paint = ofSide(mark.paint)
        return paint ? { className: paint, painted: true } : undefined
    }

    return <RawTableGrid decorate={decorate} rows={source} testId="compare-combined" />
}

export default ComparisonPanes
