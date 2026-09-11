import React from 'react'
import { Empty, Spin, Splitter } from 'antd'
import { useTranslation } from 'react-i18next'
import { RawTableGrid } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import type { ComparisonSide, ComparisonTable } from 'types/compare'
import { useStyles } from './ComparePage.styles'

interface ComparisonPanesProps {
    /** The table the user picked, or null while nothing is picked. */
    table: ComparisonTable | null
    loading: boolean
    /** Whether the rows that read the same in both files are shown. */
    showEqualRows: boolean
    error: string | null
    /** Put before the name of the first file; carries the controls the hidden list of elements left behind. */
    leading?: React.ReactNode
    /** What the two sides are called, when they are not simply the first and the second file. */
    titles?: { first: string; second: string } | undefined
}

/** The rows of one side that hold a cell reading differently in the other file. */
const changedRowsOf = (side: ComparisonSide): number[] => {
    const changed = new Set(side.changedCells ?? [])
    return side.source
        .map((row, index) => (row.some(cell => cell.cell && changed.has(cell.cell)) ? index : -1))
        .filter(index => index >= 0)
}

/**
 * The rows both files show, or null for the whole table.
 *
 * The two versions keep the same rows, whichever side a difference was found on. A row one file adds
 * is marked on that side alone, and showing it there against the whole table on the other side would
 * put the two versions out of step.
 *
 * That holds only while the versions have the same number of rows. Once they do not, the rows no
 * longer stand against each other, so both versions are shown whole. A table that differs in what it
 * is rather than in what it holds - its name, its place, its size - has no row to keep, and is shown
 * whole as well.
 */
const rowsToShow = (table: ComparisonTable, showEqualRows: boolean): ReadonlySet<number> | null => {
    const { first, second } = table
    if (showEqualRows || !first || !second || first.source.length !== second.source.length) {
        return null
    }
    const rows = new Set([...changedRowsOf(first), ...changedRowsOf(second)])
    return rows.size === 0 ? null : rows
}

/**
 * The table with only the given rows, and the merges they cut through made whole again.
 *
 * A merge that starts in a row that is gone starts in the first row that is left, and every merge
 * spans only the rows that are left. Without that a cell would keep a merge reaching rows that are no
 * longer drawn, and the columns after it would move.
 */
const keepRows = (source: RawTableCell[][], keep: ReadonlySet<number>): RawTableCell[][] => {
    const rows = source.map(row => row.map(cell => ({ ...cell })))
    rows.forEach((row, index) => row.forEach((cell, column) => {
        const span = cell.rowspan ?? 1
        if (cell.covered || span <= 1) {
            return
        }
        const kept = Array.from({ length: span }, (_, offset) => index + offset).filter(row => keep.has(row))
        if (kept.length === 0) {
            return
        }
        const [head, ...covered] = kept as [number, ...number[]]
        rows[head]![column] = { ...cell, rowspan: kept.length }
        covered.forEach(row => {
            rows[row]![column] = { covered: true }
        })
    }))
    return rows.filter((_, index) => keep.has(index))
}

/**
 * The two versions of the picked table, side by side, with the cells that differ marked.
 *
 * Each file heads its own column, on the line the controls of the comparison are on, and the divider
 * between them gives either file more room. A table only one file holds is shown on that side alone;
 * the other side says the file does not have it.
 */
export const ComparisonPanes: React.FC<ComparisonPanesProps> = ({
    table,
    loading,
    showEqualRows,
    error,
    leading,
    titles,
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    const rows = table ? rowsToShow(table, showEqualRows) : null

    const content = (side: ComparisonSide | undefined) => {
        if (loading) {
            return <div className={styles.center}><Spin /></div>
        }
        if (error) {
            return <Empty description={error} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        if (!table) {
            return <Empty description={t('nothing_selected')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        if (!side) {
            return <Empty description={t('absent')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return <Side rows={rows} side={side} />
    }

    const pane = (side: ComparisonSide | undefined, title: string, testId: string, before?: React.ReactNode) => (
        <div className={styles.column}>
            <div className={styles.head}>
                {before}
                <span className={cx(styles.headLabel)}>{title}</span>
            </div>
            <div className={styles.body} data-testid={testId}>{content(side)}</div>
        </div>
    )

    return (
        <Splitter className={styles.panes}>
            <Splitter.Panel min="20%">
                {pane(table?.first, titles?.first ?? t('file_first'), 'compare-pane-first', leading)}
            </Splitter.Panel>
            <Splitter.Panel min="20%">
                {pane(table?.second, titles?.second ?? t('file_second'), 'compare-pane-second')}
            </Splitter.Panel>
        </Splitter>
    )
}

const Side: React.FC<{ side: ComparisonSide; rows: ReadonlySet<number> | null }> = ({ side, rows }) => {
    const { styles } = useStyles()
    const changed = new Set(side.changedCells ?? [])

    const decorate = (cell: RawTableCell) => {
        const marked = !!cell.cell && changed.has(cell.cell)
        return { className: marked ? styles.changed : undefined, painted: marked }
    }

    return <RawTableGrid decorate={decorate} rows={rows ? keepRows(side.source, rows) : side.source} />
}

export default ComparisonPanes
