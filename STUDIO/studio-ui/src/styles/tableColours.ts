import type { GlobalToken } from 'antd'
import type { RawTableCell, RawTableCellBorder, RawTableCellStyle } from 'types/tables'
import { opaque } from './colorMath'
import type { TablePaper } from './paper'
import type { ThemeName } from './themes'

/** The colours of the token a table takes. */
type TableTokenKey = 'colorText' | 'colorTextSecondary' | 'colorBgContainer' | 'colorFill' | 'colorPrimaryBg'
    | 'colorPrimaryBgHover' | 'colorBorderSecondary' | 'colorLink' | 'colorLinkHover' | 'colorPrimaryTextHover'
    | 'colorError'

/** The part of the token of the application a table takes its colours from. */
type TableToken = Pick<GlobalToken, TableTokenKey>

/** The colour each key of a table theme file is drawn in, in place of the colour the table theme sets there. */
type KeyedColours = Readonly<Partial<Record<string, string>>>

/**
 * The colours a table drawn with a table theme takes from the theme of the application, where the reader asks for
 * them (**Override with Studio theme** in My Settings). They are a drawing only: no workbook and no project takes them.
 *
 * A read reports each colour of the table theme with the key of the theme file it is set at, such as
 * `spreadsheet.values.background`. A colour of a key the look of the application colours takes that colour; any other
 * colour keeps the colour of the table theme ({@link inLook}). The table theme still decides the lines, the bold and
 * the alignment, while the text is set in the font of the application.
 *
 * Every colour is solid, as the colours of a workbook are: a translucent colour of the token is given as it shows on
 * the ground of the table.
 */
export interface TableColours {
    /** The colour each key of the table theme file is drawn in, in place of the colour the table theme sets there. */
    keyed: KeyedColours
    /** The paper the table is laid on: the ground and the ink of its base style, its grid, links and notes. */
    paper: TablePaper
}

/**
 * The colour of the token each key of a table theme file takes, nested as the file nests its keys: `base`, a kind of
 * table such as `spreadsheet`, then the part, such as `values`, and the attribute, such as `background`.
 */
interface LookColours {
    readonly [key: string]: TableTokenKey | LookColours
}

/**
 * The look a theme of the application gives its tables: the table theme that lays the tables out, and the colours of
 * the theme the keys of its file take in the dark appearance. In the light appearance the keys keep the colours of the
 * table theme, which are made for a light ground.
 */
interface TableLook {
    /** The table theme the tables are read with, by its identifier: it decides the bold, the lines and alignment. */
    tableTheme: string
    /** The colours of the dark appearance. */
    dark: LookColours
}

// The dark colours of the Standard table theme, each named as its file names the colour it takes the place of. The
// blue of the titles of what a table gives is further from the ground than the light blue of what it gives, as in the
// light appearance, and both take the grounds of the primary colour.
const BLACK: TableTokenKey = 'colorText'
const WHITE: TableTokenKey = 'colorBgContainer'
const DARK_GREY: TableTokenKey = 'colorTextSecondary'
const GREY: TableTokenKey = 'colorFill'
const BLUE: TableTokenKey = 'colorPrimaryBgHover'
const LIGHT_BLUE: TableTokenKey = 'colorPrimaryBg'

// The parts several kinds share, shared as the Standard table theme file shares them by its aliases.
const MUTED: LookColours = { color: DARK_GREY }
const TITLE: LookColours = { background: GREY }
const GIVEN_TITLE: LookColours = { background: BLUE }
const GIVEN: LookColours = { background: LIGHT_BLUE }
const SIMPLE: LookColours = { titles: TITLE, returnTitles: GIVEN_TITLE, returns: GIVEN }
const LOOKUP: LookColours = { titles: TITLE, horizontals: GIVEN_TITLE, returnTitles: GIVEN_TITLE, returns: GIVEN }

/**
 * The dark colours of the Standard table theme, at the keys of its file. Every key the file sets a colour at has one:
 * `tableColours.test.ts` reads the file to check it. The kinds the file writes nothing for take the base alone, the
 * General format, so they take the colours of the base here too.
 */
const STANDARD_DARK: LookColours = {
    base: {
        style: { color: BLACK, background: WHITE },
        header: { keyword: MUTED, type: MUTED, parameters: MUTED },
        properties: MUTED,
    },
    datatype: { titles: TITLE, type: MUTED, name: GIVEN, values: MUTED },
    spreadsheet: { titles: GIVEN_TITLE, stepTitle: TITLE, values: GIVEN, sections: GIVEN_TITLE },
    data: { name: MUTED, titles: GIVEN_TITLE, values: GIVEN },
    test: { name: MUTED, titles: TITLE, returnTitles: GIVEN_TITLE, returns: GIVEN },
    rules: { code: MUTED, titles: TITLE, returnTitles: GIVEN_TITLE, returns: GIVEN },
    simpleRules: SIMPLE,
    smartRules: SIMPLE,
    simpleLookup: LOOKUP,
    smartLookup: LOOKUP,
}

/**
 * The themes of the application that have a look of their own for the tables, by their name. The Standard theme draws
 * them with the Standard table theme: in the light appearance in the colours of its file, which are made for it, and
 * in the dark one in the dark colours of the theme.
 *
 * Any theme can join with a look of its own, the colours of each key read off its own token: `tableColours.test.ts`
 * checks that its dark colours colour every key of the file of its table theme, and that every text of it stays
 * readable on every fill.
 */
const TABLE_LOOKS: Readonly<Partial<Record<ThemeName, TableLook>>> = {
    standard: { tableTheme: 'standard', dark: STANDARD_DARK },
}

/** The themes of the application that have a look of their own for the tables. */
export const LOOK_THEMES: readonly ThemeName[] = Object.keys(TABLE_LOOKS) as ThemeName[]

/**
 * The table theme the tables are read with where the reader asks for the look of the theme of the application
 * (**Override with Studio theme**): the one the look of the theme lays its tables out with, whatever table theme the
 * settings name. The table theme of the settings stays the fallback: for a reader who does not ask, and under a theme
 * with no look of its own for the tables.
 *
 * @param follow    whether the reader asks for the colours of the theme
 * @param themeName the theme of the application
 * @returns the table theme, or undefined where the tables are read with the table theme of the settings
 */
export const followedTableTheme = (follow: boolean, themeName: ThemeName): string | undefined =>
    (follow ? TABLE_LOOKS[themeName]?.tableTheme : undefined)

/** The token colour of each key of a theme file, the nested keys joined by dots. */
const keysOf = (colours: LookColours, prefix = ''): [string, TableTokenKey][] =>
    Object.entries(colours).flatMap(([key, value]) => (typeof value === 'string'
        ? [[`${prefix}${key}`, value] as [string, TableTokenKey]]
        : keysOf(value, `${prefix}${key}.`)))

/**
 * The colours of the look a theme of the application gives its tables, in the appearance the application is drawn in.
 *
 * @param themeName  the theme of the application
 * @param isDarkMode whether the application is drawn in the dark appearance
 * @param token      the token of the application
 * @returns the colours, or undefined for a theme of the application with no look of its own for the tables
 */
export const tableColoursOf = (
    themeName: ThemeName,
    isDarkMode: boolean,
    token: TableToken
): TableColours | undefined => {
    const look = TABLE_LOOKS[themeName]
    if (look === undefined) {
        return undefined
    }
    const solid = (key: TableTokenKey) => opaque(token[key], token.colorBgContainer)
    const keyed: KeyedColours = isDarkMode
        ? Object.fromEntries(keysOf(look.dark).map(([key, tokenKey]) => [key, solid(tokenKey)]))
        : {}
    // The link of the dark token reads on the ground only, not on the blue a value is filled with, and it darkens
    // under the pointer: a dark table draws its links in the lighter text of the primary colour, underlined under the
    // pointer.
    const link = solid(isDarkMode ? 'colorPrimaryTextHover' : 'colorLink')
    return {
        keyed,
        paper: {
            // The base style of the table theme is the ground and the ink of the table.
            background: keyed['base.style.background'] ?? solid('colorBgContainer'),
            text: keyed['base.style.color'] ?? solid('colorText'),
            grid: solid('colorBorderSecondary'),
            link,
            linkHover: isDarkMode ? link : solid('colorLinkHover'),
            note: solid('colorError'),
        },
    }
}

/** The colour the look draws a key in, or undefined for a key it does not colour. */
const colourAt = (keyed: KeyedColours, key: string | undefined): string | undefined =>
    (key === undefined ? undefined : keyed[key])

/** The sides of a border. */
const SIDES = ['top', 'right', 'bottom', 'left'] as const

/** The lines of the table theme around a cell, as the look draws them. */
const bordersInLook = (border: RawTableCellBorder, keyed: KeyedColours): RawTableCellBorder => {
    const drawn = { ...border }
    for (const side of SIDES) {
        const line = border[side]
        const color = colourAt(keyed, line?.colorKey)
        if (line !== undefined && color !== undefined) {
            drawn[side] = { ...line, color }
        }
    }
    return drawn
}

/**
 * A style of the table theme as the look draws it: each colour taken at its key, and the text in the font of the
 * application, so the font of the table theme is left out.
 */
const styleInLook = ({ fontFamily, fontSize, ...style }: RawTableCellStyle, keyed: KeyedColours): RawTableCellStyle => {
    const background = colourAt(keyed, style.backgroundKey)
    const color = colourAt(keyed, style.colorKey)
    return {
        ...style,
        ...(background !== undefined && { background }),
        ...(color !== undefined && { color }),
        ...(style.border !== undefined && { border: bordersInLook(style.border, keyed) }),
    }
}

/** A cell a table theme draws: a read naming the theme reports the look of the theme as the style of the cell. */
type ThemedCell = RawTableCell & { style: RawTableCellStyle }

/** Whether a table theme draws a cell: its style is the look of the theme, which names the theme as its source. */
export const inTheme = (cell: RawTableCell): cell is ThemedCell => cell.style?.source === 'theme'

/** A cell the table theme draws, as the look draws it: its style and the pieces of its text. */
const cellInLook = (cell: ThemedCell, keyed: KeyedColours): RawTableCell => ({
    ...cell,
    style: styleInLook(cell.style, keyed),
    ...(cell.runs !== undefined && {
        runs: cell.runs.map(run => (run.style === undefined ? run : { ...run, style: styleInLook(run.style, keyed) })),
    }),
})

/**
 * The cells of a table as the look of the theme of the application draws them.
 *
 * Every colour the table theme draws a cell with, the pieces of its text among them, takes the colour of the key it is
 * set at, where the look colours that key. The text takes the font of the application. A cell the table theme does
 * not draw, whose style is not the theme's, is left as it is.
 *
 * @param rows    the cells of the table, read with the table theme of the look
 * @param colours the colours of the look
 * @returns the cells as the look draws them
 */
export const inLook = (rows: RawTableCell[][], colours: TableColours): RawTableCell[][] =>
    rows.map(row => row.map(cell => (inTheme(cell) ? cellInLook(cell, colours.keyed) : cell)))
