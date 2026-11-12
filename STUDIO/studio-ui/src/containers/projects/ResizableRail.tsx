import React from 'react'
import { createStyles } from 'antd-style'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { useElementSize } from '../../hooks/useElementSize'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    /** The rail is dragged by its right edge, which the grip is laid along. */
    resizable: css`
        position: relative;
    `,
    /** Whatever narrows the list, set apart from it. */
    top: css`
        flex: none;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
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
    /** Draws the list in the room the rail has for it: no room before the rail is laid out. */
    children: (room: { height: number, width: number }) => React.ReactNode
}

/**
 * The left rail of a workspace: a list under what narrows it down, which the reader widens by its right edge.
 *
 * <p>The rail measures the room its list has, so a virtual list draws the rows that fit. The width it is dragged to
 * is kept for the next time.
 */
export const ResizableRail = ({ testId, widthKey, width, top, children }: ResizableRailProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { size, startResize } = useDragSize(widthKey, 'right', width)
    const { ref, size: room } = useElementSize<HTMLDivElement>()
    return (
        <aside className={cx(shared.rail, styles.resizable)} data-testid={testId} style={{ width: size }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId={`${testId}-resizer`} />
            {top && <div className={styles.top}>{top}</div>}
            <div ref={ref} className={styles.body}>{children(room)}</div>
        </aside>
    )
}
