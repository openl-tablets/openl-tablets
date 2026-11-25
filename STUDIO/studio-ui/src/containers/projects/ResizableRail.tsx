import React from 'react'
import { createStyles } from 'antd-style'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { useElementSize } from '../../hooks/useElementSize'
import { useSharedStyles } from './sharedStyles'
import { FoldedRail, FoldRailButton } from './RailFold'

const useStyles = createStyles(({ css, token }) => ({
    /** The rail is dragged by its right edge, which the grip is laid along. */
    resizable: css`
        position: relative;
    `,
    /** Whatever narrows the list, spaced as the header of the projects rail, and the handle that folds the rail. */
    top: css`
        flex: none;
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        padding: ${token.paddingSM}px ${token.padding}px;

        & > :first-child {
            flex: 1;
            min-width: 0;
        }
    `,
    /**
     * The list scrolls inside itself: it draws the rows in sight and no more, so a list of thousands of rows scrolls
     * as fast as one of ten.
     */
    body: css`
        flex: 1;
        min-height: 0;
        overflow: hidden;
        padding: 4px 8px 12px;
    `,
}))

interface ResizableRailProps {
    /** Names the rail, and its grip with `-resizer` after it. */
    testId: string
    /** Where the width the rail was last dragged to is kept, apart from the other rails. */
    widthKey: string
    width: { min: number, max: number, fallback: number }
    /** What stands over the list and narrows it: how it is grouped, what it leaves out. The header searches it. */
    top?: React.ReactNode
    /**
     * Whether the rail is folded away, and what folds or unfolds it. A rail given it carries the handle that folds it
     * at the end of its top row, and leaves a narrow strip in its place.
     */
    fold?: [folded: boolean, setFolded: (folded: boolean) => void]
    /** The name of the rail, written down the strip it leaves when it is folded away. */
    name?: string
    /** Draws the list in the room the rail has for it: no room before the rail is laid out. */
    children: (room: { height: number, width: number }) => React.ReactNode
}

/**
 * The left rail of a workspace: a list under what narrows it down, which the reader widens by its right edge.
 *
 * <p>The rail measures the room its list has, so a virtual list draws the rows that fit. The width it is dragged to
 * is kept for the next time, and so is a rail folded away.
 */
export const ResizableRail = ({ testId, widthKey, width, top, fold, name = '', children }: ResizableRailProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { size, startResize } = useDragSize(widthKey, 'right', width)
    const { ref, size: room } = useElementSize<HTMLDivElement>()
    if (fold?.[0]) {
        return <FoldedRail name={name} onUnfold={() => fold[1](false)} testId={testId} />
    }
    return (
        <aside className={cx(shared.rail, styles.resizable)} data-testid={testId} style={{ width: size }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId={`${testId}-resizer`} />
            {(top || fold) && (
                <div className={styles.top}>
                    {top}
                    {fold && <FoldRailButton onFold={() => fold[1](true)} testId={testId} />}
                </div>
            )}
            <div ref={ref} className={styles.body}>{children(room)}</div>
        </aside>
    )
}
