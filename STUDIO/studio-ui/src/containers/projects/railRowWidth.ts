/** What one step of the hierarchy costs in width, matching the indent of a rail tree (`railTree` of `sharedStyles`). */
export const RAIL_INDENT = 12
/** The switcher, the icon and the padding a row carries before its name. */
const ROW_CHROME = 56
/** What one character of a name takes, at the font size of a rail tree. */
const CHARACTER = 7.2

/**
 * How wide a row of a rail tree is, its indent left out: the text it shows and what it draws beside the text.
 *
 * A virtualised tree draws a screenful of rows, so it is told how far it scrolls sideways rather than finding out
 * from the rows it draws. The width is reckoned from the text rather than measured in the page: a reckoning a few
 * pixels over is a few pixels of empty room, not a clipped name.
 */
export const railRowWidth = (text: string, extra = 0): number => ROW_CHROME + text.length * CHARACTER + extra
