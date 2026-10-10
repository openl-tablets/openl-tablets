import type { GlobalToken } from 'antd'
import type {
    ExcelThemeColorName,
    RawTableCell,
    RawTableCellBorder,
    RawTableCellStyle,
    RawTableThemeColor,
} from 'types/tables'
import { mix, opaque } from './colorMath'
import { paperToken, type TablePaper, workbookPaper } from './paper'
import { variantOf, type ThemeName } from './themes'

/**
 * The ten theme colours of Excel, as a theme of OpenL Studio gives them: the first and the second background and text,
 * `lt1`, `dk1`, `lt2` and `dk2`, and the six accents.
 */
export type ExcelPalette = Readonly<Record<ExcelThemeColorName, string>>

/**
 * The ten theme colours of Office 2013 - 2022, the theme of a new workbook of Excel. A colour picked for a cell is shown
 * in them until the table is saved and read again in the colours of its own workbook.
 */
export const OFFICE_PALETTE: ExcelPalette = {
    lt1: '#ffffff',
    dk1: '#000000',
    lt2: '#e7e6e6',
    dk2: '#44546a',
    accent1: '#4472c4',
    accent2: '#ed7d31',
    accent3: '#a5a5a5',
    accent4: '#ffc000',
    accent5: '#5b9bd5',
    accent6: '#70ad47',
}

/**
 * How the tables of the workbooks are drawn under the theme in force. It is carried on the antd-style theme as
 * `table` (`styles/customToken.ts`), so every screen draws a table alike and a style reads its colours from there.
 *
 * A table is drawn formatted with the table theme, whose every colour is one of the sixty colours the palette of Excel
 * offers, as the server reports it. The tables are drawn in the colours the theme of OpenL Studio gives those ten theme
 * colours ({@link palette}), dark in the dark appearance, unless the reader asks for the formatting of the Excel file
 * (**Show Original Excel Formatting** in My Settings). That is drawn as Excel draws it, black on white, whatever the
 * theme.
 */
export interface TableTheme {
    /** The palette the cells are drawn in, or undefined where they keep the colours of their Excel file. */
    palette?: ExcelPalette
    /** The paper the table is laid on: its ground and ink, its grid, its links and the marks of its notes. */
    paper: TablePaper
    /**
     * The token a mark a screen lays on the cells takes, such as the outline of a picked cell or the fill of a
     * changed one: the theme's own, or Ant Design's light one on the paper of an Excel file, so a mark reads on the
     * paper it lies on.
     */
    marks: GlobalToken
}

/** A translucent colour of the token, as it shows on the container it lies on. */
const solid = (token: GlobalToken, colour: string): string => opaque(colour, token.colorBgContainer)

/**
 * The ten theme colours of Excel as a theme of OpenL Studio gives them in an appearance.
 *
 * The first background and text are the container and the text of the theme, and the second ones its secondary fill
 * and text. The first accent is its primary colour, and the other five the accents of its code editor, each of the
 * hue Excel's own accent has, so a table keeps its hues in every theme.
 *
 * @param name       the theme of OpenL Studio
 * @param isDarkMode whether the theme is drawn in its dark appearance
 * @param token      the token of the theme in that appearance
 * @returns the palette
 */
export const excelPaletteOf = (name: ThemeName, isDarkMode: boolean, token: GlobalToken): ExcelPalette => ({
    lt1: token.colorBgContainer,
    dk1: solid(token, token.colorTextBase),
    lt2: solid(token, token.colorFillSecondary),
    dk2: solid(token, token.colorTextSecondary),
    accent1: token.colorPrimary,
    ...variantOf(name, isDarkMode).accents,
})

/** The colours worked out for each palette, by the colours a tint mixes and the tint. */
const tinted = new Map<string, string>()

/**
 * The colour a colour of the palette of Excel is drawn in, in the colours of a palette.
 *
 * A tint makes the theme colour lighter or darker as Excel does, but in the colours of the palette: lighter moves it
 * towards the first background, darker towards the first text. In a dark appearance a lighter fill is therefore a
 * quieter one, closer to the ground, as it is on the white ground of Excel.
 *
 * @param palette the palette
 * @param colour  the colour of the palette of Excel
 * @returns the colour as `#rrggbb`
 */
export const colourIn = (palette: ExcelPalette, { name, tint = 0 }: RawTableThemeColor): string => {
    const base = palette[name]
    if (tint === 0) {
        return base
    }
    const towards = tint > 0 ? palette.lt1 : palette.dk1
    const key = `${base}:${towards}:${tint}`
    let colour = tinted.get(key)
    if (colour === undefined) {
        colour = mix(base, towards, Math.abs(tint))
        tinted.set(key, colour)
    }
    return colour
}

/** A colour of a style in the colours of a palette: by its colour of the palette of Excel, where it has one. */
const repainted = (palette: ExcelPalette, colour: string | undefined, theme: RawTableThemeColor | undefined) =>
    (theme === undefined ? colour : colourIn(palette, theme))

/** The sides of a border. */
const SIDES = ['top', 'right', 'bottom', 'left'] as const

/** The lines around a cell in the colours of a palette. */
const borderIn = (palette: ExcelPalette, border: RawTableCellBorder): RawTableCellBorder => {
    const drawn: RawTableCellBorder = { ...border }
    for (const side of SIDES) {
        const line = border[side]
        if (line?.colorTheme !== undefined) {
            drawn[side] = { ...line, color: colourIn(palette, line.colorTheme) }
        }
    }
    return drawn
}

/** A style in the colours of a palette: its fill, its text and its lines. */
export const styleIn = (palette: ExcelPalette, style: RawTableCellStyle): RawTableCellStyle => {
    const background = repainted(palette, style.background, style.backgroundTheme)
    const color = repainted(palette, style.color, style.colorTheme)
    return {
        ...style,
        ...(background !== undefined && { background }),
        ...(color !== undefined && { color }),
        ...(style.border !== undefined && { border: borderIn(palette, style.border) }),
    }
}

/** A cell in the colours of a palette: its style and the pieces of its text. */
const cellIn = (palette: ExcelPalette, cell: RawTableCell): RawTableCell => (cell.style === undefined
    && cell.runs === undefined
    ? cell
    : {
        ...cell,
        ...(cell.style !== undefined && { style: styleIn(palette, cell.style) }),
        ...(cell.runs !== undefined && {
            runs: cell.runs.map(run => (run.style === undefined
                ? run
                : { ...run, style: styleIn(palette, run.style) })),
        }),
    })

/**
 * The cells of a table in the colours a table theme draws them in.
 *
 * Every colour of a cell, a piece of its text and a line around it takes the colour of the palette its colour of the
 * palette of Excel is drawn in. A colour reported without one keeps its own. Where the table theme keeps the colours of
 * the Excel file, the cells are left as they are.
 *
 * @param rows  the cells of the table
 * @param table how the tables are drawn
 * @returns the cells as they are drawn
 */
export const paintedIn = (rows: RawTableCell[][], table: TableTheme): RawTableCell[][] => {
    const { palette } = table
    return palette === undefined ? rows : rows.map(row => row.map(cell => drawnIn(palette, cell)))
}

/** The cells drawn in each palette, by the cell given: a screen that changes one cell draws that one cell anew. */
const drawnCells = new WeakMap<ExcelPalette, WeakMap<RawTableCell, RawTableCell>>()

/** A cell in the colours of a palette, drawn once for as long as the screen gives the same cell. */
const drawnIn = (palette: ExcelPalette, cell: RawTableCell): RawTableCell => {
    let drawn = drawnCells.get(palette)
    if (drawn === undefined) {
        drawn = new WeakMap()
        drawnCells.set(palette, drawn)
    }
    let painted = drawn.get(cell)
    if (painted === undefined) {
        painted = cellIn(palette, cell)
        drawn.set(cell, painted)
    }
    return painted
}

/**
 * How the tables are drawn under a theme of OpenL Studio in an appearance.
 *
 * @param name        the theme of OpenL Studio
 * @param isDarkMode  whether the theme is drawn in its dark appearance
 * @param token       the token of the theme in that appearance
 * @param excelFormatting whether the reader asks for the colours of the Excel file, as Excel draws them
 * @returns how the tables are drawn
 */
export const tableThemeOf = (name: ThemeName, isDarkMode: boolean, token: GlobalToken,
    excelFormatting: boolean): TableTheme => {
    if (excelFormatting) {
        return workbookTable()
    }
    const palette = excelPaletteOf(name, isDarkMode, token)
    // The link of the dark token reads on the ground only, not on the blue a value is filled with, and it darkens
    // under the pointer: a dark table draws its links in the lighter text of the primary colour, underlined under the
    // pointer.
    const link = solid(token, isDarkMode ? token.colorPrimaryTextHover : token.colorLink)
    return {
        palette,
        paper: {
            background: palette.lt1,
            text: palette.dk1,
            grid: solid(token, token.colorBorderSecondary),
            link,
            linkHover: isDarkMode ? link : solid(token, token.colorLinkHover),
            note: token.colorError,
        },
        marks: token,
    }
}

/** How a table is drawn as Excel draws it, worked out the first time it is asked for. */
let asInExcel: TableTheme | undefined

/**
 * How a table is drawn as Excel draws it: in the formatting and the colours of its file, black on white on the paper of
 * a workbook, whatever the theme.
 *
 * @returns how the table is drawn
 */
export const workbookTable = (): TableTheme => {
    asInExcel ??= { paper: workbookPaper(), marks: paperToken() }
    return asInExcel
}

/** The tables of each token no theme provider carries them on, worked out once. */
const unprovided = new WeakMap<GlobalToken, TableTheme>()

/**
 * How the tables are drawn under the theme a style or a component is given.
 *
 * The theme provider of the application carries it on the theme ({@link TableTheme}). A part drawn without that
 * provider, such as one a test draws alone, draws its tables in the colours of the standard theme of its token.
 *
 * @param theme the token of the theme, with what the application carries on it
 * @returns how the tables are drawn
 */
export const tableOf = (theme: GlobalToken & { table?: TableTheme }): TableTheme => {
    if (theme.table !== undefined) {
        return theme.table
    }
    let table = unprovided.get(theme)
    if (table === undefined) {
        table = tableThemeOf('standard', false, theme, false)
        unprovided.set(theme, table)
    }
    return table
}
