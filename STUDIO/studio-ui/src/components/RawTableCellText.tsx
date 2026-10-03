import React from 'react'
import { Tooltip } from 'antd'
import type { RawTableCell, RawTableCellUsage, RawTableTextRun } from 'types/tables'
import type { RawTableGridStyles } from './RawTableGrid.styles'
import { runStyle, type RunPlace } from './rawTableStyle'

/** Where a usage leads, when the screen showing the table can follow it. */
export type OpenUsage = (usage: RawTableCellUsage) => void

interface RawTableCellTextProps {
    /** The text of the cell as it is shown — what the ranges of the usages are measured over. */
    text: string
    /** What the compiler knows about the cell; without it the text is shown as it stands. */
    metaInfo: RawTableCell['metaInfo']
    /** The table's styles, read once by the table: a cell of a long table reads none of its own. */
    styles: RawTableGridStyles
    /** Follows a usage to the table it names; absent when this screen cannot go there. */
    onOpenUsage?: OpenUsage | undefined
    /** The pieces of the text formatted with fonts of their own; absent when the text has one font. */
    runs?: RawTableTextRun[] | undefined
    /** The cell is beside the point on this screen, so the colours of its runs are drawn in grey. */
    muted?: boolean | undefined
}

/** A run and where it stands in the text shown. */
interface PlacedRun {
    start: number
    end: number
    run: RawTableTextRun
}

/**
 * Where each run stands in the text shown, or undefined when the runs do not spell that text.
 *
 * The text shown is the value of the cell, which leaves out the spaces around it, while the runs carry the text
 * as the workbook writes it.
 */
const placeRuns = (text: string, runs: RawTableTextRun[] | undefined): PlacedRun[] | undefined => {
    if (!runs?.length) {
        return undefined
    }
    const offset = runs.map(run => run.text).join('').indexOf(text)
    if (offset < 0) {
        return undefined
    }
    let at = -offset
    return runs.map(run => {
        const placed = { start: at, end: at + run.text.length, run }
        at = placed.end
        return placed
    })
}

/** A slice of the text, drawn in the fonts of the runs it crosses. */
const styledSlice = (text: string, start: number, end: number, placed: PlacedRun[] | undefined,
    place: RunPlace): React.ReactNode => {
    if (placed === undefined) {
        return text.slice(start, end)
    }
    return placed
        .filter(({ start: from, end: to }) => from < end && to > start)
        .map(({ start: from, end: to, run }) => (
            <span key={from} style={runStyle(run, place)}>
                {text.slice(Math.max(from, start), Math.min(to, end))}
            </span>
        ))
}

/**
 * The text of a cell, with the pieces the compiler resolved marked as the Editor marks them.
 *
 * Each piece says what it stands for in a tooltip, and one that names a table is a way into that table.
 * The cell a decision table returns carries the star the Editor drew for it.
 */
export const RawTableCellText: React.FC<RawTableCellTextProps> = ({
    text,
    metaInfo,
    styles,
    onOpenUsage,
    runs,
    muted = false,
}) => {
    const usages = metaInfo?.usages ?? []
    const placed = placeRuns(text, runs)
    const slice = (start: number, end: number, linked = false) =>
        styledSlice(text, start, end, placed, { muted, linked })

    if (usages.length === 0 && !metaInfo?.returnCell) {
        return <>{slice(0, text.length)}</>
    }

    const pieces: React.ReactNode[] = []
    let read = 0
    usages.forEach((usage, index) => {
        const start = Math.max(read, Math.min(usage.start, text.length))
        const end = Math.max(start, Math.min(usage.end, text.length))
        if (start > read) {
            pieces.push(<React.Fragment key={`text-${read}`}>{slice(read, start)}</React.Fragment>)
        }
        const leads = Boolean(usage.tableId && usage.module && onOpenUsage)
        // A piece that leads somewhere is drawn as a link, whatever colour the workbook gives its text.
        const covered = slice(start, end, leads)
        pieces.push(
            <Tooltip key={`${usage.start}-${index}`} title={usage.description}>
                {leads ? (
                    <button
                        className={styles.usageLink}
                        data-testid={`cell-usage-${index}`}
                        onClick={() => onOpenUsage?.(usage)}
                        type="button"
                    >
                        {covered}
                    </button>
                ) : (
                    <span data-testid={`cell-usage-${index}`}>{covered}</span>
                )}
            </Tooltip>
        )
        read = end
    })
    if (read < text.length) {
        pieces.push(<React.Fragment key={`text-${read}`}>{slice(read, text.length)}</React.Fragment>)
    }
    if (metaInfo?.returnCell) {
        pieces.push(
            <Tooltip key="return" title="RETURN">
                <span data-testid="cell-return"> ★</span>
            </Tooltip>
        )
    }

    return <>{pieces}</>
}

export default RawTableCellText
