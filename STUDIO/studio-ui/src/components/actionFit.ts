import { useCallback, useLayoutEffect, useRef, useState, type DependencyList } from 'react'
import { createStyles } from 'antd-style'

/** Gap between the buttons of a bar, in px: the bars lay their buttons out with it, so the fit matches the layout. */
export const ACTION_GAP = 8
/** The look of a bar whose actions collapse into a menu, and of the copy of them it measures. */
export const useActionBarStyles = createStyles(({ css }) => ({
    bar: css`
        position: relative;
        display: flex;
        flex: none;
        flex-wrap: nowrap;
        align-items: center;
        justify-content: flex-end;
        gap: ${ACTION_GAP}px;
    `,
    /** An off-screen copy of every action, laid out at its natural width, read to decide what fits. */
    measure: css`
        position: absolute;
        top: 0;
        left: 0;
        display: flex;
        gap: ${ACTION_GAP}px;
        visibility: hidden;
        pointer-events: none;
    `,
}))

/**
 * How many leading actions fit in `available` px. When they do not all fit, room for the overflow trigger
 * of `moreWidth` is kept, and the first (primary) action is always shown, however little space there is.
 */
export const fitActionCount = (widths: number[], moreWidth: number, available: number, gap: number): number => {
    if (widths.length === 0) {
        return 0
    }
    const total = widths.reduce((sum, w) => sum + w, 0) + gap * (widths.length - 1)
    if (total <= available) {
        return widths.length
    }
    let used = 0
    let count = 0
    for (let index = 0; index < widths.length; index++) {
        const next = used + widths[index]! + (index > 0 ? gap : 0)
        if (next + gap + moreWidth > available) {
            break
        }
        used = next
        count = index + 1
    }
    return Math.max(1, count)
}

/**
 * How many leading actions of a bar fit in the row it stands in, the rest collapsing into a menu.
 *
 * The bar lays an off-screen copy of every action out at its natural width under `measureRef`, its last child being the
 * trigger of the menu, and stands itself under `barRef`; the row is the element the bar stands in. The fit is worked
 * out again as the row resizes, and whenever `deps` change — another set of actions.
 *
 * Before the browser has laid the bar out, and in jsdom, which reports no widths, every action is shown.
 *
 * @param count how many actions the bar has
 * @param moreAlways whether the trigger of the menu stands in the bar whatever fits, holding actions of its own
 */
export const useActionFit = (count: number, deps: DependencyList, { moreAlways = false } = {}) => {
    const barRef = useRef<HTMLDivElement>(null)
    const measureRef = useRef<HTMLDivElement>(null)
    const [visible, setVisible] = useState(count)

    const recompute = useCallback(() => {
        const measure = measureRef.current
        const bar = barRef.current
        const row = bar?.parentElement
        if (!measure || !bar || !row) {
            return
        }
        const nodes = Array.from(measure.children) as HTMLElement[]
        const widths = nodes.slice(0, count).map(node => node.offsetWidth)
        const moreWidth = nodes.at(-1)?.offsetWidth ?? 0
        if (row.clientWidth <= 0 || widths.some(width => width <= 0)) {
            setVisible(count)
            return
        }
        const available = row.clientWidth
        setVisible(moreAlways
            ? fitActionCount(widths, 0, available - moreWidth - ACTION_GAP, ACTION_GAP)
            : fitActionCount(widths, moreWidth, available, ACTION_GAP))
        // eslint-disable-next-line react-hooks/exhaustive-deps -- the callers name what the fit depends on
    }, [count, moreAlways, ...deps])

    useLayoutEffect(() => {
        recompute()
        const row = barRef.current?.parentElement
        if (!row || typeof ResizeObserver === 'undefined') {
            return
        }
        const observer = new ResizeObserver(() => recompute())
        observer.observe(row)
        return () => observer.disconnect()
    }, [recompute])

    return { barRef, measureRef, visible: Math.min(visible, count) }
}
