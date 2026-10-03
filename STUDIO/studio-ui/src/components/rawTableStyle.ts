import type React from 'react'
import type { RawTableCellBorder, RawTableCellBorderSide, RawTableCellStyle, RawTableTextRun } from 'types/tables'
import { paperToken } from '../styles/paper'

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
 * on ({@link paperToken}), whatever the theme. A muted cell is left to draw it in its grey.
 */
const automaticColour = (muted: boolean): string | undefined => (muted ? undefined : paperToken().colorText)

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
const line = (side: RawTableCellBorderSide | undefined, muted: boolean): string | undefined => {
    if (side === undefined) {
        return undefined
    }
    const drawn = `${side.width ?? 1}px ${side.style ?? 'solid'}`
    const colour = tinted(side.color, muted) ?? automaticColour(muted)
    return colour === undefined ? drawn : `${drawn} ${colour}`
}

/** The font a style names, with a sans-serif one to fall back on where the machine does not have it. */
export const fontFamilyOf = (style: RawTableCellStyle | undefined): string | undefined =>
    (style?.fontFamily === undefined ? undefined : `"${style.fontFamily}", sans-serif`)

/** The size of the font a style names, in points. */
export const fontSizeOf = (style: RawTableCellStyle | undefined): string | undefined =>
    (style?.fontSize === undefined ? undefined : `${style.fontSize}pt`)

/** The borders of a cell the workbook draws none around, shared by every such cell. */
const NO_BORDERS: React.CSSProperties = {}

/**
 * The borders the workbook draws around a cell. A side it does not name keeps the line of the grid.
 *
 * @param border the borders of the cell
 * @param muted  whether the cell is beside the point on this screen
 */
export const borders = (border: RawTableCellBorder | undefined, muted: boolean): React.CSSProperties =>
    (border === undefined ? NO_BORDERS : {
        borderTop: line(border.top, muted),
        borderRight: line(border.right, muted),
        borderBottom: line(border.bottom, muted),
        borderLeft: line(border.left, muted),
    })

/** Where a run is drawn. */
export interface RunPlace {
    /** The cell is beside the point on this screen, so the colours of its runs are drawn in grey. */
    muted: boolean
    /** The run stands in a link, which keeps its own colour and underline. */
    linked: boolean
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
            : tinted(style.color, place.muted) ?? automaticColour(place.muted),
        fontWeight: style.bold ? 'bold' : 'normal',
        fontStyle: style.italic ? 'italic' : 'normal',
        textDecoration: place.linked ? undefined : textDecoration(style) ?? 'none',
        fontFamily: fontFamilyOf(style),
        fontSize: fontSizeOf(style),
    }
}
