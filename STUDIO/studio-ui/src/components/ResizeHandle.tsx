import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { createStyles } from 'antd-style'
import { readStored, writeStored } from '../utils/localStore'

/** The edge of a panel the reader drags. The opposite edge stands still, and the panel grows towards this one. */
export type ResizeEdge = 'top' | 'bottom' | 'left' | 'right'

/** How far a panel may be dragged, and the size it takes when it has not been dragged yet. */
interface DragBounds {
    min: number
    max: number
    fallback: number
}

/** The size the pointer asks for: the distance from the edge that stands still to the pointer itself. */
const sizeAt = (edge: ResizeEdge, panel: DOMRect, pointer: PointerEvent): number => {
    if (edge === 'bottom') {
        return pointer.clientY - panel.top
    }
    if (edge === 'top') {
        return panel.bottom - pointer.clientY
    }
    return edge === 'right' ? pointer.clientX - panel.left : panel.right - pointer.clientX
}

/**
 * The size a panel was last dragged to, and the drag that changes it.
 *
 * The size is kept in this browser under the given key, so a panel opens where the reader left it rather than
 * at its default every time. A stored size outside the bounds is ignored, which is what happens when the bounds
 * themselves change.
 */
export const useDragSize = (storageKey: string, edge: ResizeEdge, { min, max, fallback }: DragBounds) => {
    const [size, setSize] = useState(() => {
        const stored = Number(readStored(storageKey))
        return Number.isFinite(stored) && stored >= min && stored <= max ? stored : fallback
    })

    // A drag is followed on the window, so it survives the pointer leaving the grip. It must not survive the
    // panel itself: a screen that goes away mid-drag would leave the window listening for good.
    const endDrag = useRef(() => {})
    useEffect(() => () => endDrag.current(), [])

    const startResize = useCallback((event: ReactPointerEvent<HTMLHRElement>) => {
        event.preventDefault()
        const panel = (event.currentTarget.parentElement ?? event.currentTarget).getBoundingClientRect()
        const bounded = (moved: PointerEvent) => Math.min(max, Math.max(min, Math.round(sizeAt(edge, panel, moved))))
        const resize = (moved: PointerEvent) => setSize(bounded(moved))
        const stop = (moved: PointerEvent) => {
            resize(moved)
            endDrag.current()
            writeStored(storageKey, String(bounded(moved)))
        }
        endDrag.current = () => {
            window.removeEventListener('pointermove', resize)
            window.removeEventListener('pointerup', stop)
        }
        window.addEventListener('pointermove', resize)
        window.addEventListener('pointerup', stop)
    }, [edge, max, min, storageKey])

    return { size, startResize }
}

const useStyles = createStyles(({ css, token }) => ({
    /** The grip widens on hover, so the edge can be grabbed without aiming at it. */
    handle: css`
        position: absolute;
        margin: 0;
        border: none;
        background: transparent;
        touch-action: none;
        z-index: 2;

        &:hover,
        &:active {
            background: ${token.colorPrimaryBorder};
        }
    `,
    top: css`
        top: -3px;
        right: 0;
        left: 0;
        height: 6px;
        cursor: row-resize;
    `,
    bottom: css`
        right: 0;
        bottom: -3px;
        left: 0;
        height: 6px;
        cursor: row-resize;
    `,
    left: css`
        top: 0;
        bottom: 0;
        left: -3px;
        width: 6px;
        height: auto;
        cursor: col-resize;
    `,
    right: css`
        top: 0;
        right: 0;
        bottom: 0;
        width: 6px;
        height: auto;
        cursor: col-resize;
    `,
}))

interface ResizeHandleProps {
    /** Which edge of the panel this grip is; the panel itself must be positioned. */
    edge: ResizeEdge
    onPointerDown: (event: ReactPointerEvent<HTMLHRElement>) => void
    testId: string
}

/** The edge of a panel, drawn as something to take hold of. */
export const ResizeHandle = ({ edge, onPointerDown, testId }: ResizeHandleProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    return (
        <hr
            aria-label={t('browser.compile.resize')}
            className={cx(styles.handle, styles[edge])}
            data-testid={testId}
            onPointerDown={onPointerDown}
        />
    )
}
