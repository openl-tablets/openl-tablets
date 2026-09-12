import React from 'react'
import { Tooltip } from 'antd'
import type { RawTableCell, RawTableCellUsage } from 'types/tables'
import { useStyles } from './RawTableGrid.styles'

/** Where a usage leads, when the screen showing the table can follow it. */
export type OpenUsage = (usage: RawTableCellUsage) => void

interface RawTableCellTextProps {
    /** The text of the cell as it is shown — what the ranges of the usages are measured over. */
    text: string
    /** What the compiler knows about the cell; without it the text is shown as it stands. */
    metaInfo: RawTableCell['metaInfo']
    /** Follows a usage to the table it names; absent when this screen cannot go there. */
    onOpenUsage?: OpenUsage | undefined
}

/**
 * The text of a cell, with the pieces the compiler resolved marked as the Editor marks them.
 *
 * Each piece says what it stands for in a tooltip, and one that names a table is a way into that table.
 * The cell a decision table returns carries the star the Editor drew for it.
 */
export const RawTableCellText: React.FC<RawTableCellTextProps> = ({ text, metaInfo, onOpenUsage }) => {
    const { styles } = useStyles()
    const usages = metaInfo?.usages ?? []

    if (usages.length === 0 && !metaInfo?.returnCell) {
        return <>{text}</>
    }

    const pieces: React.ReactNode[] = []
    let read = 0
    usages.forEach((usage, index) => {
        const start = Math.max(read, Math.min(usage.start, text.length))
        const end = Math.max(start, Math.min(usage.end, text.length))
        if (start > read) {
            pieces.push(text.slice(read, start))
        }
        const covered = text.slice(start, end)
        const leads = Boolean(usage.tableId && usage.module && onOpenUsage)
        pieces.push(
            <Tooltip key={`${usage.start}-${index}`} title={usage.description}>
                <span
                    className={leads ? styles.usageLink : undefined}
                    data-testid={`cell-usage-${index}`}
                    onClick={leads ? () => onOpenUsage?.(usage) : undefined}
                >
                    {covered}
                </span>
            </Tooltip>
        )
        read = end
    })
    if (read < text.length) {
        pieces.push(text.slice(read))
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
