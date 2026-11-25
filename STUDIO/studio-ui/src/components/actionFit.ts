import { useCallback, useLayoutEffect, useRef, useState, type DependencyList } from 'react'
import { createStyles } from 'antd-style'

/** Gap between the buttons of a bar, in px: the bars lay their buttons out with it, so the fit matches the layout. */
export const ACTION_GAP = 8
/** The gap of the title row a bar sits in, in px, as the head of a workspace lays it out. */
const TITLE_ROW_GAP = 12
/** The least room a title keeps beside the bar, however many actions it has. */
const TITLE_MIN = 80

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
 * The width the title beside a bar takes when nothing is cut from it: what its parts take, and what an ellipsis hides
 * of them.
 */
const naturalWidth = (title: HTMLElement): number => {
    const parts = Array.from(title.children) as HTMLElement[]
    const shown = parts.reduce((sum, part) => sum + part.offsetWidth, 0) + TITLE_ROW_GAP * Math.max(0, parts.length - 1)
    const cut = Array.from(title.querySelectorAll<HTMLElement>('*'))
        .reduce((most, element) => Math.max(most, element.scrollWidth - element.clientWidth), 0)
    return shown + cut
}

/**
 * The room the title beside a bar keeps: the whole of it, so its name reads in full, but never more than half the row,
 * and never less than a short name needs.
 */
const titleRoom = (row: HTMLElement, bar: HTMLElement): number => {
    const title = Array.from(row.children)
        .find(child => child !== bar && !child.contains(bar)) as HTMLElement | undefined
    const natural = title === undefined ? 0 : naturalWidth(title)
    return Math.max(TITLE_MIN, Math.min(natural, row.clientWidth / 2))
}

/**
 * How many leading actions of a bar beside a title fit in the row, the rest collapsing into a menu.
 *
 * The bar lays an off-screen copy of every action out at its natural width under `measureRef`, its last child being the
 * trigger of the menu, and stands itself under `barRef`. With `wholeTitle` the title keeps its whole width, up to half
 * the row, so collapsing actions shows the name of what is open rather than cutting it short; otherwise it keeps the
 * least room a short name needs. The fit is worked out again as the row resizes, and whenever `deps` change — another
 * set of actions, or another title.
 *
 * Before the browser has laid the bar out, and in jsdom, which reports no widths, every action is shown.
 *
 * @param count how many actions the bar has
 * @param moreAlways whether the trigger of the menu stands in the bar whatever fits, holding actions of its own
 * @param wholeTitle whether the title keeps its whole name beside the bar, up to half the row
 */
export const useActionFit = (count: number, deps: DependencyList, { moreAlways = false, wholeTitle = false } = {}) => {
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
        const available = row.clientWidth - (wholeTitle ? titleRoom(row, bar) : TITLE_MIN) - TITLE_ROW_GAP
        setVisible(moreAlways
            ? fitActionCount(widths, 0, available - moreWidth - ACTION_GAP, ACTION_GAP)
            : fitActionCount(widths, moreWidth, available, ACTION_GAP))
        // eslint-disable-next-line react-hooks/exhaustive-deps -- the callers name what the fit depends on
    }, [count, moreAlways, wholeTitle, ...deps])

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
