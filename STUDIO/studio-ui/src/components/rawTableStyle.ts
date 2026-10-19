import type React from 'react'
import type {
    RawTableCell,
    RawTableCellBorder,
    RawTableCellBorderSide,
    RawTableCellStyle,
    RawTableTextRun,
} from 'types/tables'

/** How much of its brightness a muted colour keeps. */
const MUTED_BRIGHTNESS = 0.8

const HEX_COLOUR = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i

/**
 * The grey a colour reads as when its cell is beside the point: the brightness of the colour itself,
 * dimmed, so that what the cell is filled with still tells light from dark.
 *
 * A colour written in any other way is left alone, and so is a colour the cell does not carry - an
 * unfilled cell stays unfilled rather than turning grey.
 */
const mute = (colour: string | undefined): string | undefined => {
    if (!colour || !HEX_COLOUR.test(colour)) {
        return colour
    }
    const digits = colour.length === 4
        ? Array.from(colour.slice(1), digit => digit + digit).join('')
        : colour.slice(1)
    const value = Number.parseInt(digits, 16)
    const average = (((value >> 16) & 0xff) + ((value >> 8) & 0xff) + (value & 0xff)) / 3
    const grey = Math.round(average * MUTED_BRIGHTNESS)
    return `rgb(${grey}, ${grey}, ${grey})`
}

/** A colour of the cell as it is drawn: its own, or the grey it reads as when the cell is muted. */
export const tinted = (colour: string | undefined, muted: boolean): string | undefined =>
    (muted ? mute(colour) : colour)

/**
 * The colour Excel draws a line or a piece of text in when it names none: the ink of the paper the table is written
 * on, whatever the theme. A muted cell is left to draw it in its grey.
 */
const automaticColour = (muted: boolean, ink: string): string | undefined => (muted ? undefined : ink)

/** The lines drawn through and under a text, or undefined when there are none. */
export const textDecoration = (style: RawTableCellStyle | undefined): string | undefined => {
    if (style?.underline) {
        return style.strikeout ? 'underline line-through' : 'underline'
    }
    return style?.strikeout ? 'line-through' : undefined
}

/**
 * One side of a border as the browser draws it.
 *
 * A side the workbook names no colour for is drawn in the automatic colour, as Excel draws it. A colour the text of
 * the cell has does not reach the border, except in a muted cell, whose lines are drawn in its grey.
 */
const line = (side: RawTableCellBorderSide | undefined, muted: boolean, ink: string): string | undefined => {
    if (side === undefined) {
        return undefined
    }
    const drawn = `${side.width ?? 1}px ${side.style ?? 'solid'}`
    const colour = tinted(side.color, muted) ?? automaticColour(muted, ink)
    return colour === undefined ? drawn : `${drawn} ${colour}`
}

/** The borders of a cell the workbook draws none around, shared by every such cell. */
const NO_BORDERS: React.CSSProperties = {}

/**
 * The borders the workbook draws around a cell. A side it does not name keeps the line of the grid.
 *
 * @param border the borders of the cell
 * @param muted  whether the cell is beside the point on this screen
 * @param ink    the ink of the paper the table is written on, which a line that names no colour is drawn in
 */
export const borders = (border: RawTableCellBorder | undefined, muted: boolean, ink: string): React.CSSProperties =>
    (border === undefined ? NO_BORDERS : {
        borderTop: line(border.top, muted, ink),
        borderRight: line(border.right, muted, ink),
        borderBottom: line(border.bottom, muted, ink),
        borderLeft: line(border.left, muted, ink),
    })

/** The width of a line in pixels, a line naming none being as thin as a line gets. */
const widthOf = (side: RawTableCellBorderSide): number => side.width ?? 1

/**
 * The line drawn on an edge two cells name a line for: the wider of the two, as Excel draws it, and the first where
 * they are as wide. Either may be absent.
 */
export const wider = (first: RawTableCellBorderSide | undefined, second: RawTableCellBorderSide | undefined):
    RawTableCellBorderSide | undefined => {
    if (first === undefined) {
        return second
    }
    return second === undefined || widthOf(second) <= widthOf(first) ? first : second
}

/** The sides of a cell a collapse is drawing, before it is known whether the cell draws any. */
interface Sides {
    top?: RawTableCellBorderSide | undefined
    right?: RawTableCellBorderSide | undefined
    bottom?: RawTableCellBorderSide | undefined
    left?: RawTableCellBorderSide | undefined
}

/** The place of the cell that holds each place of the table: the cell itself, or the merged cell covering it. */
const holdersOf = (rows: RawTableCell[][]): Array<Array<[number, number] | undefined>> => {
    const holders: Array<Array<[number, number] | undefined>> = rows.map(row => row.map(() => undefined))
    rows.forEach((row, r) => row.forEach((cell, c) => {
        if (cell.covered) {
            return
        }
        for (let below = r; below < r + (cell.rowspan ?? 1) && below < rows.length; below++) {
            for (let right = c; right < c + (cell.colspan ?? 1); right++) {
                const places = holders[below]
                if (places !== undefined && right < places.length) {
                    places[right] = [r, c]
                }
            }
        }
    }))
    return holders
}

/** The borders a collapse drew for a cell, or undefined for a cell drawing none. */
const bordersOf = (sides: Sides): RawTableCellBorder | undefined => {
    const border: RawTableCellBorder = {}
    if (sides.top !== undefined) {
        border.top = sides.top
    }
    if (sides.right !== undefined) {
        border.right = sides.right
    }
    if (sides.bottom !== undefined) {
        border.bottom = sides.bottom
    }
    if (sides.left !== undefined) {
        border.left = sides.left
    }
    return Object.keys(border).length === 0 ? undefined : border
}

/** The borders a cell names itself: the ones of its Excel style. */
const workbookBordersOf = (cell: RawTableCell): RawTableCellBorder | undefined => cell.style?.border

/**
 * The borders each cell of a table is drawn with, each line two cells share drawn once.
 *
 * <p>Every cell names the lines it draws itself, so a line between two cells may be named by both, or by either. The
 * browser draws the side of the cell above or on the left over the side of its neighbour, and the line of the grid
 * where that cell names none, so a line is moved onto that cell: the bottom of the upper cell and the right of the left
 * one, the wider line winning. Only a cell along the top or the left edge of the table keeps a line above or on its
 * left. A merged cell takes the lines of every cell along its edges.
 *
 * @param rows    the cells of the table, row by row
 * @param ownOf   the lines a cell names itself: the borders of its Excel style, unless told otherwise
 * @returns the borders of each cell at its place, undefined for a cell drawing none and for a covered place
 */
export const collapsedBorders = (
    rows: RawTableCell[][],
    ownOf: (cell: RawTableCell, row: number, column: number) => RawTableCellBorder | undefined = workbookBordersOf
): Array<Array<RawTableCellBorder | undefined>> => {
    const holders = holdersOf(rows)
    const owns = rows.map((row, r) => row.map((cell, c) => (cell.covered ? undefined : ownOf(cell, r, c))))
    const sides: Array<Array<Sides | undefined>> = rows.map((row, r) => row.map((cell, c) => {
        const own = owns[r]?.[c]
        return cell.covered ? undefined : {
            right: own?.right,
            bottom: own?.bottom,
            top: r === 0 ? own?.top : undefined,
            left: c === 0 ? own?.left : undefined,
        }
    }))
    const at = (place: [number, number] | undefined): Sides | undefined =>
        place === undefined ? undefined : sides[place[0]]?.[place[1]]
    // The line above a cell moves onto the bottom of each cell above it; with none there, the cell keeps it.
    const moveTop = (r: number, c: number, span: number, line: RawTableCellBorderSide, drawn: Sides) => {
        for (let column = c; column < c + span; column++) {
            const above = at(holders[r - 1]?.[column])
            if (above === undefined) {
                drawn.top = wider(drawn.top, line)
            } else {
                above.bottom = wider(above.bottom, line)
            }
        }
    }
    // The line on the left of a cell moves onto the right of each cell before it, as the line above does.
    const moveLeft = (r: number, c: number, span: number, line: RawTableCellBorderSide, drawn: Sides) => {
        for (let row = r; row < r + span && row < rows.length; row++) {
            const before = at(holders[row]?.[c - 1])
            if (before === undefined) {
                drawn.left = wider(drawn.left, line)
            } else {
                before.right = wider(before.right, line)
            }
        }
    }
    rows.forEach((row, r) => row.forEach((cell, c) => {
        const own = owns[r]?.[c]
        const drawn = sides[r]?.[c]
        if (cell.covered || own === undefined || drawn === undefined) {
            return
        }
        if (r > 0 && own.top !== undefined) {
            moveTop(r, c, cell.colspan ?? 1, own.top, drawn)
        }
        if (c > 0 && own.left !== undefined) {
            moveLeft(r, c, cell.rowspan ?? 1, own.left, drawn)
        }
    }))
    return sides.map(row => row.map(cell => (cell === undefined ? undefined : bordersOf(cell))))
}

/** Where a run is drawn. */
export interface RunPlace {
    /** The cell is beside the point on this screen, so the colours of its runs are drawn in grey. */
    muted: boolean
    /** The run stands in a link, which keeps its own colour and underline. */
    linked: boolean
    /** The ink of the paper the table is written on, which a run that names no colour is drawn in. */
    ink: string
}

/**
 * The font a run draws its text with.
 *
 * A run with a font of its own names every attribute of it, so an attribute it leaves out is at its default rather
 * than taken from the cell. A colour it leaves out is the automatic one, as Excel draws it; in a muted cell it keeps the
 * grey of the cell. A run in a link takes its font only, so the link reads as a link. A run
 * without a font takes the font of the cell.
 */
export const runStyle = (run: RawTableTextRun, place: RunPlace): React.CSSProperties | undefined => {
    const style = run.style
    if (style === undefined) {
        return undefined
    }
    return {
        color: place.linked
            ? undefined
            : tinted(style.color, place.muted) ?? automaticColour(place.muted, place.ink),
        fontWeight: style.bold ? 'bold' : 'normal',
        fontStyle: style.italic ? 'italic' : 'normal',
        textDecoration: place.linked ? undefined : textDecoration(style) ?? 'none',
    }
}

/**
 * The indent of a cell, as the room it leaves beside its text: one em for each step the workbook gives.
 *
 * A cell aligned right is indented from the right, as Excel draws it; any other from the left. A cell without an
 * indent takes no room of its own.
 */
export const indentOf = (style: RawTableCellStyle | undefined): React.CSSProperties => {
    if (!style?.indent) {
        return {}
    }
    const side = style.align === 'right' ? '--cell-indent-right' : '--cell-indent-left'
    return { [side]: `${style.indent}em` }
}
