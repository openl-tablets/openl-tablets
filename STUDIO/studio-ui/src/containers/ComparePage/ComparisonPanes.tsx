import React from 'react'
import { Empty, Spin, Splitter } from 'antd'
import { useTranslation } from 'react-i18next'
import { RawTableGrid, type CellDecoration } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import type { ComparisonSide, ComparisonTable } from 'types/compare'
import { combine, type CellMark, type CombinedTable } from './combinedDiff'
import { keepRows, rowsToShow } from './diffRows'
import { useStyles } from './ComparePage.styles'

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
}

/**
 * The two versions of the picked table, side by side or drawn as one table, with the cells that
 * differ marked.
 *
 * Side by side, each file heads its own column, on the line the controls of the comparison are on, and
 * the divider between them gives either file more room. A table only one file holds is shown on that
 * side alone; the other side says the file does not have it.
 *
 * Drawn as one, the two versions share a table: a cell the files read differently carries both values,
 * and every row is led by the sign it is read by.
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
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    const rows = table ? rowsToShow(table, showEqualRows) : null

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

    const content = (side: ComparisonSide | undefined) => insteadOfTable()
        ?? (side
            ? <Side rows={rows} side={side} />
            : <Empty description={t('absent')} image={Empty.PRESENTED_IMAGE_SIMPLE} />)

    const pane = (head: React.ReactNode, testId: string, body: React.ReactNode) => (
        <div className={styles.column}>
            <div className={styles.head}>{head}</div>
            <div className={styles.body} data-testid={testId}>{body}</div>
        </div>
    )

    const named = (title: string, before?: React.ReactNode, after?: React.ReactNode) => (
        <>
            {before}
            <span className={cx(styles.headLabel)}>{title}</span>
            {after && <span className={styles.headAction}>{after}</span>}
        </>
    )

    if (view === 'combined') {
        const combined = table ? combine(table, rows) : null
        const head = (
            <>
                {leading}
                <span className={cx(styles.headLabel)}>
                    {`${titles?.first ?? t('file_first')} ${SIGNS.changed} ${titles?.second ?? t('file_second')}`}
                </span>
                {(combined || trailing) && (
                    <span className={styles.headTail}>
                        {combined && <Legend of={combined} />}
                        {trailing}
                    </span>
                )}
            </>
        )
        const body = insteadOfTable() ?? <Combined combined={combined!} />
        return pane(head, 'compare-pane-combined', body)
    }

    return (
        <Splitter className={styles.panes}>
            <Splitter.Panel min="20%">
                {pane(named(titles?.first ?? t('file_first'), leading), 'compare-pane-first',
                    content(table?.first))}
            </Splitter.Panel>
            <Splitter.Panel min="20%">
                {pane(named(titles?.second ?? t('file_second'), undefined, trailing), 'compare-pane-second',
                    content(table?.second))}
            </Splitter.Panel>
        </Splitter>
    )
}

const Side: React.FC<{ side: ComparisonSide; rows: ReadonlySet<number> | null }> = ({ side, rows }) => {
    const { styles } = useStyles()
    const changed = new Set(side.changedCells ?? [])

    // What differs is what the reader came for, so the rest of the table steps back into grey. A table
    // that differs in nothing has nothing to step back from and keeps the colours the workbook gives it.
    const decorate = (cell: RawTableCell) => {
        const marked = !!cell.cell && changed.has(cell.cell)
        return {
            className: marked ? styles.changed : undefined,
            painted: marked,
            muted: !marked && changed.size > 0,
        }
    }

    return <RawTableGrid decorate={decorate} rows={rows ? keepRows(side.source, rows) : side.source} />
}

/** The sign a row of the combined view is read by. */
const SIGNS: Record<CellMark['row'], string> = { equal: '', changed: '→', added: '+', removed: '−' }

/** The signs a combined table is read by, in the order they are explained. */
const SIGNED: CellMark['row'][] = ['changed', 'added', 'removed']

/** What the signs leading the rows of this table mean, for the reader who meets them for the first time. */
const Legend: React.FC<{ of: CombinedTable }> = ({ of }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const used = new Set([...of.marks.values()].map(mark => mark.row))
    const kinds = SIGNED.filter(kind => used.has(kind))

    return kinds.length === 0 ? null : (
        <span className={styles.legend} data-testid="compare-legend">
            {kinds.map(kind => (
                <span key={kind}>
                    <span className={styles.legendSign}>{SIGNS[kind]}</span>
                    {t(`status_${kind}`)}
                </span>
            ))}
        </span>
    )
}

/**
 * The two versions as one table: a cell the files read differently carries both values, and the sign
 * leading each row says what became of it.
 */
const Combined: React.FC<{ combined: CombinedTable }> = ({ combined }) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    const { rows: source, marks } = combined
    // A table that differs in nothing has nothing to step back from, as in the view beside this one.
    const differs = [...marks.values()].some(mark => mark.before !== undefined || mark.row !== 'equal')

    /** The colour a row that is wholly of one file or the other is drawn in. */
    const ofSide = (kind: CellMark['row']) => {
        if (kind === 'added') {
            return styles.add
        }
        return kind === 'removed' ? styles.remove : undefined
    }

    const decorate = (cell: RawTableCell): CellDecoration | undefined => {
        const mark = marks.get(cell)
        if (!mark) {
            return undefined
        }
        if (mark.lead) {
            return {
                className: cx(styles.combinedLead, ofSide(mark.row)),
                painted: true,
                // The sign says what became of the row, and says so in words to whoever hovers it. A
                // row that reads the same in both files carries no sign, so there is nothing to hover.
                content: SIGNS[mark.row] && <span title={t(`status_${mark.row}`)}>{SIGNS[mark.row]}</span>,
            }
        }
        if (mark.before !== undefined) {
            return {
                className: styles.changed,
                painted: true,
                content: (
                    <>
                        <span className={styles.combinedBefore}>{mark.before}</span>
                        {` ${SIGNS.changed} `}
                        <span>{mark.after}</span>
                    </>
                ),
            }
        }
        // Only a row wholly of one file or the other is painted over; a row that merely changed keeps
        // what the workbook fills its cells with, muted like every other cell that does not differ.
        const paint = ofSide(mark.row)
        return { className: paint, painted: !!paint, muted: differs }
    }

    return <RawTableGrid decorate={decorate} rows={source} testId="compare-combined" />
}

export default ComparisonPanes
