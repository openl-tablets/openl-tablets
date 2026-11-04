import React, { useMemo } from 'react'
import { Badge, Empty, Flex, Spin, Splitter, Tag, Typography } from 'antd'
import { SwapOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { RawTableGrid, type CellDecoration } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import type {
    ComparisonLine,
    ComparisonLineStatus,
    ComparisonSide,
    ComparisonTable,
    ConflictComparisonLine,
    ConflictComparisonTable,
} from 'types/compare'
import { alignmentOf, alignSide, ownStatusOf, type Alignment, type FileSide } from './alignment'
import { combine, combineConflict, type CellMark, type CombinedTable } from './combinedDiff'
import { keepRows, keptRows, rowsToShow, type ShownLine } from './diffRows'
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

/** What a legend explains: a kind of change, or the cells both versions of a conflict changed differently. */
type LegendKind = ChangeKind | 'conflict'

/** The colour of the dot a legend marks a kind with, matching the colour its cells are painted or framed in. */
const DOTS: Record<LegendKind, 'warning' | 'success' | 'error'> = {
    changed: 'warning',
    added: 'success',
    removed: 'error',
    conflict: 'error',
}

/** What each colour means, each kind led by the sign of its rows when the versions are drawn as one table. */
const Legend: React.FC<{ kinds: LegendKind[]; combined?: boolean }> = ({ kinds, combined = false }) => {
    const { t } = useTranslation('compare')
    return (
        <Flex data-testid="compare-legend" gap="small">
            {kinds.map(kind => {
                const label = t(`status_${kind}`)
                return (
                    <Badge
                        key={kind}
                        status={DOTS[kind]}
                        text={combined && kind !== 'conflict' ? `${SIGNS[kind]} ${label}` : label}
                    />
                )
            })}
        </Flex>
    )
}

/** What is shown instead of a table: the comparison is not ready, it failed, or nothing is picked. */
const NoTable: React.FC<{ loading: boolean; error: string | null }> = ({ loading, error }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    if (loading) {
        return <div className={styles.center}><Spin /></div>
    }
    return <Empty description={error || t('nothing_selected')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
}

/** A column of the comparison: the line its version is named on, with the controls, over what it shows. */
const Pane: React.FC<{ head: React.ReactNode; testId: string; children: React.ReactNode }> = ({
    head,
    testId,
    children,
}) => {
    const { styles } = useStyles()
    return (
        <div className={styles.column}>
            <div className={styles.head}>{head}</div>
            <div className={styles.body} data-testid={testId}>{children}</div>
        </div>
    )
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
    const alignment = useMemo(() => (table ? alignmentOf(table) : null), [table])
    const rows = useMemo(() => rowsToShow(alignment, showEqualRows), [alignment, showEqualRows])
    const combined = useMemo(() => (view === 'combined' && table ? combine(table, rows) : null), [view, table, rows])
    const shown = !loading && !error && table
    const insteadOfTable = shown ? null : <NoTable error={error} loading={loading} />

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
        const body = insteadOfTable ?? (
            <Restyled restyled={table!.formattingChanged}>
                <Combined combined={combined!} />
            </Restyled>
        )
        return withNotes(<div className={styles.panes}><Pane head={head} testId="compare-pane-combined">{body}</Pane></div>)
    }

    const content = (side: FileSide) => {
        const source = table?.[side]
        return insteadOfTable
            ?? (source
                ? (
                    <Restyled restyled={table?.formattingChanged}>
                        <Side alignment={alignment} rows={rows} side={side} source={source} />
                    </Restyled>
                )
                : <Empty description={t('absent')} image={Empty.PRESENTED_IMAGE_SIMPLE} />)
    }

    // The pane of a file; the controls lead the left pane and end the right one, whichever file each pane shows.
    const sidePane = (side: FileSide, left: boolean) => (
        <Pane
            testId={`compare-pane-${side}`}
            head={(
                <>
                    {left && leading}
                    <span className={cx(styles.headLabel)}>{titles?.[side] ?? t(`file_${side}`)}</span>
                    {!left && trailing && <span className={styles.headAction}>{trailing}</span>}
                </>
            )}
        >
            {content(side)}
        </Pane>
    )
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
            {kinds.length > 0 && <Legend combined={combined} kinds={kinds} />}
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

/** How a cell of a version is framed: changed, or changed differently by both versions of a conflict. */
type CellFrame = 'changed' | 'conflict'

/** The class of a framed cell, framed along the sides its frame runs. */
const useFrame = () => {
    const { styles, cx } = useCellStyles()
    return (sides: FrameSides, kind: CellFrame = 'changed') =>
        cx(kind === 'conflict' ? styles.conflict : styles.changedFrame, sidesOf(styles, sides))
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
 * are. A cell both versions of a conflict changed differently is framed in red.
 */
const Side: React.FC<{
    source: ComparisonSide
    side: FileSide
    alignment: Alignment | null
    rows: ShownLine[] | null
    conflicts?: ReadonlySet<string> | undefined
}> = ({ source, side, alignment, rows, conflicts }) => {
    const { styles } = useCellStyles()
    const frame = useFrame()
    const gapOf = useGap()
    /** How a cell is framed, by its address: a changed cell stands on lines both versions hold. */
    const frameOfCell = useMemo(() => {
        const changed = new Set(source.changedCells ?? [])
        return (cell: RawTableCell): CellFrame | undefined => {
            if (!cell.cell) {
                return undefined
            }
            if (conflicts?.has(cell.cell)) {
                return 'conflict'
            }
            return changed.has(cell.cell) ? 'changed' : undefined
        }
    }, [source, conflicts])
    // The rows drawn, laid out over the lines both files hold, with their frames.
    const drawn = useMemo(() => {
        if (!alignment) {
            return null
        }
        const lines = rows ?? alignment.rows.map((_, line): ShownLine => ({ line }))
        const kept = keepRows(alignSide(source.source, alignment, side), lines, alignment.columns.length)
        return {
            ...kept,
            shownRows: lines.map(line => ('line' in line ? alignment.rows[line.line] : undefined)),
            frames: framesOf(kept.rows, frameOfCell),
        }
    }, [source, alignment, rows, side, frameOfCell])
    const own = ownStatusOf(side)
    const paint = own === 'added' ? styles.rowAdded : styles.rowRemoved

    if (!alignment || !drawn) {
        // A table only this file holds is all of it added or removed.
        const decorate = (): CellDecoration => ({ className: paint, painted: true })
        return <RawTableGrid decorate={decorate} rows={source.source} />
    }

    const { rows: shown, gaps, shownRows, frames } = drawn
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
        return sides && { className: frame(sides, frameOfCell(cell)) }
    }

    return <RawTableGrid decorate={decorate} rows={shown} />
}

/** Which version of a conflict a column shows, from left to right as merge tools lay them out. */
export type ConflictPane = 'yours' | 'base' | 'theirs'

const CONFLICT_PANES: ConflictPane[] = ['yours', 'base', 'theirs']

/**
 * The two versions of a conflict compared with the base they were made from, without the base itself: the version a
 * merge goes into first, then the version it brings in. Yours first when their changes come into your branch.
 */
export const VERSION_PANES: ConflictPane[] = ['yours', 'theirs']

/** The two versions without the base when your changes go into their branch: theirs first. */
export const THEIRS_FIRST_PANES: ConflictPane[] = ['theirs', 'yours']

/** How each version of a conflict is named above its column. */
const PANE_TITLES: Record<ConflictPane, string> = { yours: 'your_version', base: 'base_version', theirs: 'their_version' }

/** Whether a line of a conflict is one a version changed, removed or inserted. */
const differs = (line: ConflictComparisonLine): boolean => line.yoursStatus !== 'equal' || line.theirsStatus !== 'equal'

/**
 * The lines of a conflict as one column reads them, the way a side of two files reads them. The base is the first
 * side: a line either version removed is removed there, and a line a version inserted stands as an empty line. A
 * version is the second side: a line it inserted is added, and a line the other version inserted stands as an empty
 * line.
 */
const paneLines = (lines: ConflictComparisonLine[], pane: ConflictPane): ComparisonLine[] => lines.map(line => {
    if (pane === 'base') {
        const removed = line.yoursStatus === 'removed' || line.theirsStatus === 'removed'
        return { first: line.base ?? null, status: removed ? 'removed' : 'equal' }
    }
    const status = pane === 'yours' ? line.yoursStatus : line.theirsStatus
    return { second: line[pane] ?? null, status }
})

/** The rows and the columns of a conflict as one column reads them, or null for a table no version shares. */
const paneAlignment = (table: ConflictComparisonTable, pane: ConflictPane): Alignment | null => (table.rows?.length
    ? { rows: paneLines(table.rows, pane), columns: paneLines(table.columns ?? [], pane) }
    : null)

/**
 * The rows of a conflict to draw, as {@link rowsToShow} picks the rows of two files: a row differs when either
 * version changed, removed or inserted it, and every row is drawn once either version added or removed a column.
 */
const conflictRowsToShow = (table: ConflictComparisonTable, showEqualRows: boolean): ShownLine[] | null => {
    const columnsDiffer = (table.columns ?? []).some(column => [column.yoursStatus, column.theirsStatus]
        .some(status => status === 'added' || status === 'removed'))
    return table.rows?.length && !columnsDiffer ? keptRows(table.rows.map(differs), showEqualRows) : null
}

/** The kinds of change a conflict shows, for the legend above its columns. */
const conflictKindsOf = (table: ConflictComparisonTable): LegendKind[] => {
    const lines = [...(table.rows ?? []), ...(table.columns ?? [])]
    const statuses = new Set(lines.flatMap(line => [line.yoursStatus, line.theirsStatus]))
    const changed = !!(table.yours?.changedCells?.length || table.theirs?.changedCells?.length)
    const conflict = !!(table.yours?.conflictCells?.length || table.theirs?.conflictCells?.length)
    return [
        ...KINDS.filter(kind => (kind === 'changed' ? changed : statuses.has(kind))),
        ...(conflict ? ['conflict' as const] : []),
    ]
}

interface ConflictPanesProps {
    /** The table the user picked, or null while nothing is picked. */
    table: ConflictComparisonTable | null
    loading: boolean
    showEqualRows: boolean
    error: string | null
    /** Put before the name of your version; carries the controls the hidden list of elements left behind. */
    leading?: React.ReactNode
    /** The versions drawn side by side, from left to right; all three unless the base is left out. */
    panes?: ConflictPane[] | undefined
    /** Whether the versions stand side by side or are drawn as one table. */
    view?: DiffView | undefined
    /** Put at the end of the line the versions are named on; the control that picks the view. */
    trailing?: React.ReactNode
}

/**
 * The versions of a table of a conflict, side by side as merge tools lay them out: your version on the left, the base
 * in the middle and their version on the right. Without the base, your version and theirs stand side by side, each
 * compared with the base, or are drawn as one table.
 *
 * The rows and the columns of the versions stand against each other by the lines of the base. A line a version
 * inserted is painted as added in it and stands against empty lines in the others; a line a version removed is painted
 * as removed in the base and stands against an empty line in that version. The cells each version changed are framed,
 * and the cells both changed differently are framed in red. A table only some versions hold is shown in those.
 *
 * Drawn as one table, a cell one version changed carries the base value and the new one, and a cell both changed
 * differently carries the base value and the values of both, framed in red.
 */
export const ConflictPanes: React.FC<ConflictPanesProps> = ({
    table,
    loading,
    showEqualRows,
    error,
    leading,
    panes: shownPanes = CONFLICT_PANES,
    view = 'sides',
    trailing,
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    const rows = useMemo(() => (table ? conflictRowsToShow(table, showEqualRows) : null), [table, showEqualRows])
    const combined = useMemo(() => (view === 'combined' && table ? combineConflict(table, rows) : null), [view, table, rows])
    // How each column reads the conflict: its lines and the cells both versions changed differently.
    const panes = useMemo(() => new Map(CONFLICT_PANES.map(pane => [pane, {
        alignment: table && paneAlignment(table, pane),
        conflicts: table?.[pane]?.conflictCells ? new Set(table[pane].conflictCells) : undefined,
    }])), [table])

    const content = (pane: ConflictPane) => {
        if (loading || error || !table) {
            return <NoTable error={error} loading={loading} />
        }
        const source = table[pane]
        if (!source) {
            return <Empty description={t('absent')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return (
            <Side
                alignment={panes.get(pane)?.alignment ?? null}
                conflicts={panes.get(pane)?.conflicts}
                rows={rows}
                side={pane === 'base' ? 'first' : 'second'}
                source={source}
            />
        )
    }

    const kinds = table && !loading && !error ? conflictKindsOf(table) : []
    const legend = kinds.length > 0 && (
        <Flex wrap align="center" className={styles.notes} gap="small">
            <Legend combined={view === 'combined'} kinds={kinds} />
        </Flex>
    )
    if (view === 'combined') {
        // Your version and theirs, in the order they stand side by side.
        const versionOrder = shownPanes.filter(pane => pane !== 'base')
        return (
            <div className={styles.column}>
                {legend}
                <div className={styles.panes}>
                    <Pane
                        testId="compare-pane-combined"
                        head={(
                            <>
                                {leading}
                                <span className={cx(styles.headLabel)}>
                                    {`${t('base_version')} ${SIGNS.changed} ${t(PANE_TITLES[versionOrder[0]!])} `}
                                    <SwapOutlined />
                                    {` ${t(PANE_TITLES[versionOrder[1]!])}`}
                                </span>
                                {trailing && <span className={styles.headAction}>{trailing}</span>}
                            </>
                        )}
                    >
                        {combined
                            ? <Combined combined={combined} order={versionOrder} />
                            : <NoTable error={error} loading={loading} />}
                    </Pane>
                </div>
            </div>
        )
    }
    return (
        <div className={styles.column}>
            {legend}
            <Splitter className={styles.panes}>
                {shownPanes.map((pane, index) => (
                    <Splitter.Panel key={pane} min="15%">
                        <Pane
                            testId={`compare-pane-${pane}`}
                            head={(
                                <>
                                    {index === 0 && leading}
                                    <span className={cx(styles.headLabel)}>{t(PANE_TITLES[pane])}</span>
                                    {index === shownPanes.length - 1 && trailing && (
                                        <span className={styles.headAction}>{trailing}</span>
                                    )}
                                </>
                            )}
                        >
                            {content(pane)}
                        </Pane>
                    </Splitter.Panel>
                ))}
            </Splitter>
        </div>
    )
}

/** How a cell of the combined view is framed: changed, or changed differently by both versions of a conflict. */
const frameOfMark = (mark: CellMark | undefined): CellFrame | undefined => {
    if (mark?.theirs !== undefined || mark?.conflict) {
        return 'conflict'
    }
    return mark?.before === undefined ? undefined : 'changed'
}

/**
 * The two versions as one table: a cell the files read differently carries both values, and the sign
 * leading each row says what became of it.
 *
 * A cell both versions of a conflict changed differently is framed in red. It carries the base value on a line of its
 * own, and under it the value of each version, named, in the order the versions stand.
 */
const Combined: React.FC<{ combined: CombinedTable; order?: ConflictPane[] | undefined }> = ({
    combined,
    order = VERSION_PANES,
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useCellStyles()
    const frame = useFrame()
    const gapOf = useGap()
    const { rows: source, marks } = combined
    const frames = useMemo(() => framesOf(source, cell => frameOfMark(marks.get(cell))), [source, marks])

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
        if (sides && mark.conflict) {
            // A line one version removed while the other one changed it: painted as removed, framed as a conflict.
            return { className: cx(ofSide(mark.paint), frame(sides, 'conflict')), painted: true }
        }
        if (sides && mark.theirs !== undefined) {
            // Both values under the base one, each named, so neither reads as what the cell ends up holding.
            const [first, second] = order.map(pane => (
                <Flex key={pane} align="baseline" data-testid={`compare-combined-${pane}`} gap={4}>
                    <span className={styles.combinedVersion}>{t(pane === 'yours' ? 'yours_short' : 'theirs_short')}</span>
                    <span>{pane === 'yours' ? mark.after : mark.theirs}</span>
                </Flex>
            ))
            return {
                className: frame(sides, 'conflict'),
                content: (
                    <Flex vertical gap={2}>
                        <span>
                            <span className={styles.combinedBefore}>{mark.before}</span>
                            {` ${SIGNS.changed}`}
                        </span>
                        <Flex wrap align="center" gap="small">
                            {first}
                            <SwapOutlined className={styles.combinedConflict} title={t('status_conflict')} />
                            {second}
                        </Flex>
                    </Flex>
                ),
            }
        }
        if (sides) {
            return {
                className: frame(sides, frameOfMark(mark)),
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
