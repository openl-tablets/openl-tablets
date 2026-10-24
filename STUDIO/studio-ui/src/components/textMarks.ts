import { contrastRatio, mix } from 'styles/colorMath'
import { STANDARD_COLOURS } from 'styles/excelColours'
import type { RawTableCellStyle, RawTableTextRun, RawTextMark, RawTextMarkColor } from 'types/tables'

/** The colours a piece of a cell text is marked in, in the order the editor offers them. */
export const MARK_COLORS: readonly RawTextMarkColor[] = ['error', 'warning', 'success', 'info']

/** A standard colour of Excel by its name, as `#rrggbb`. */
function standard(name: string): string {
    return (STANDARD_COLOURS.find(colour => colour.name === name)?.rgb ?? '').toLowerCase()
}

/**
 * The standard colour of Excel each mark is written in: Red, Orange, Green and Light Blue. A piece of a cell text
 * that is bold in one of them is a mark.
 */
export const MARK_RGB: Readonly<Record<RawTextMarkColor, string>> = {
    error: standard('red'),
    warning: standard('orange'),
    success: standard('green'),
    info: standard('light_blue'),
}

/** The colour each mark is drawn in on a screen. */
export type MarkInks = Readonly<Record<RawTextMarkColor, string>>

/** The contrast a mark keeps on the grounds it is drawn on, as the text of the application does on its surfaces. */
const READABLE = 4.5

/** How far an ink is moved towards white or black at a time, until it reads. */
const STEP = 0.05

/** An ink moved towards white or black as little as it takes to read on every ground. */
const readable = (ink: string, text: string, grounds: readonly string[]): string => {
    for (let share = 0; share < 1; share += STEP) {
        const moved = mix(ink, text, share)
        if (grounds.every(ground => contrastRatio(moved, ground) >= READABLE)) {
            return moved
        }
    }
    return text
}

/**
 * The colours a mark is drawn in in the Studio theme: the functional colours of the theme for text, darkened or
 * lightened where they would not read on a ground they are drawn on.
 *
 * @param token   the colours of the theme
 * @param grounds the opaque colours the marks are drawn on
 */
export const markInksOf = (token: {
    colorErrorText: string
    colorWarningText: string
    colorSuccessText: string
    colorInfoText: string
}, grounds: readonly string[]): MarkInks => {
    // Towards white on a dark ground and black on a light one, which keeps the hue of the mark the longest.
    const ground = grounds[0] ?? '#ffffff'
    const text = contrastRatio(ground, '#ffffff') > contrastRatio(ground, '#000000') ? '#ffffff' : '#000000'
    return {
        error: readable(token.colorErrorText, text, grounds),
        warning: readable(token.colorWarningText, text, grounds),
        success: readable(token.colorSuccessText, text, grounds),
        info: readable(token.colorInfoText, text, grounds),
    }
}

/** The mark a font draws: bold in one of the colours of the marks. */
export const markOf = (style: RawTableCellStyle | undefined): RawTextMarkColor | undefined => {
    const color = style?.color?.toLowerCase()
    return style?.bold === true ? MARK_COLORS.find(mark => MARK_RGB[mark] === color) : undefined
}

/** Joins each mark with the next one when it is of the same colour and starts where it ends. */
export const joinMarks = (marks: readonly RawTextMark[]): RawTextMark[] =>
    marks.reduce<RawTextMark[]>((joined, mark) => {
        const last = joined.at(-1)
        if (last !== undefined && last.color === mark.color && last.to === mark.from) {
            joined[joined.length - 1] = { ...last, to: mark.to }
        } else if (mark.to > mark.from) {
            joined.push(mark)
        }
        return joined
    }, [])

/** No marks, shared by every text that has none. */
const NO_MARKS: RawTextMark[] = []

/** The marks of a cell text, read from the pieces of the text the workbook formats in fonts of their own. */
export const marksOf = (runs: readonly RawTableTextRun[] | undefined): RawTextMark[] => {
    if (runs === undefined || runs.length === 0) {
        return NO_MARKS
    }
    let from = 0
    const marks: RawTextMark[] = []
    for (const run of runs ?? []) {
        const to = from + run.text.length
        const color = markOf(run.style)
        if (color !== undefined) {
            marks.push({ from, to, color })
        }
        from = to
    }
    return joinMarks(marks)
}

/** Whether two lists of joined marks mark the same pieces in the same colours. */
export const sameMarks = (one: readonly RawTextMark[], other: readonly RawTextMark[]): boolean =>
    one.length === other.length
    && one.every((mark, index) => mark.from === other[index]?.from && mark.to === other[index]?.to
        && mark.color === other[index]?.color)

/**
 * Marks a piece of a text in a colour, or takes the marks off it.
 *
 * @param marks the marks of the text
 * @param from  the index of the first character of the piece
 * @param to    the index after the last character of the piece
 * @param color the colour of the mark, or null to take the marks off the piece
 * @returns the marks of the text afterwards
 */
export const markRange = (marks: readonly RawTextMark[], from: number, to: number,
    color: RawTextMarkColor | null): RawTextMark[] => {
    const kept = marks.flatMap(mark => [
        { ...mark, to: Math.min(mark.to, from) },
        { ...mark, from: Math.max(mark.from, to) },
    ]).filter(mark => mark.to > mark.from)
    const added = color === null || to <= from ? kept : [...kept, { from, to, color }]
    return joinMarks(added.sort((one, other) => one.from - other.from))
}

/**
 * The pieces of a text with its marks drawn over them: a marked piece is bold in the ink of its mark, the rest keeps
 * the font of the piece it stands in.
 *
 * @param text  the text of the cell
 * @param runs  the pieces the text is drawn in, or undefined for a text in the font of its cell
 * @param marks the marks of the text
 * @param inks  the colour each mark is drawn in
 */
export const markedRuns = (text: string, runs: readonly RawTableTextRun[] | undefined,
    marks: readonly RawTextMark[], inks: MarkInks): RawTableTextRun[] => {
    const pieces = runs ?? [{ text }]
    const cuts = new Set<number>([0, text.length, ...marks.flatMap(mark => [mark.from, mark.to])])
    let from = 0
    for (const piece of pieces) {
        cuts.add(from)
        from += piece.text.length
    }
    const edges = [...cuts].filter(at => at <= text.length).sort((one, other) => one - other)
    const styleAt = (at: number): RawTableCellStyle | undefined => {
        let end = 0
        return pieces.find(piece => (end += piece.text.length) > at)?.style
    }
    return edges.slice(1).map((to, index) => {
        const start = edges[index] ?? 0
        const mark = marks.find(found => found.from <= start && start < found.to)
        if (mark !== undefined) {
            return { text: text.slice(start, to), style: { bold: true, color: inks[mark.color] }, mark: mark.color }
        }
        const style = styleAt(start)
        return style === undefined ? { text: text.slice(start, to) } : { text: text.slice(start, to), style }
    })
}
