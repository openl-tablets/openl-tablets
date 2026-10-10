import { describe, expect, it } from 'vitest'
import { contrastRatio, mix } from './colorMath'
import { paperToken, workbookPaper } from './paper'
import { colourIn, excelPaletteOf, paintedIn, tableOf, tableThemeOf, type ExcelPalette } from './tableColours'
import { THEME_ORDER, THEMES, type ThemeName } from './themes'
import type { RawTableCell, RawTableThemeColor } from 'types/tables'
import { tokenFor } from '../testing/theme'

/** Every theme in both appearances. */
const LOOKS: [ThemeName, boolean][] = THEME_ORDER.flatMap(name => [[name, false], [name, true]] as [ThemeName, boolean][])

const paletteOf = (name: ThemeName, isDarkMode: boolean): ExcelPalette =>
    excelPaletteOf(name, isDarkMode, tokenFor(name, isDarkMode))

// The colours the formatting standard of OpenL tables takes, as OpenL Studio writes them into a workbook.
const GREY_TITLE: RawTableThemeColor = { name: 'lt1', tint: -0.25 }
const MUTED: RawTableThemeColor = { name: 'lt1', tint: -0.5 }
const BLUE_TITLE: RawTableThemeColor = { name: 'accent1', tint: 0.6 }
const VALUES: RawTableThemeColor = { name: 'accent5', tint: 0.8 }

describe('tableColours', () => {
    it('gives the ten theme colours of Excel the colours of the theme', () => {
        const token = tokenFor('standard', false)
        const palette = excelPaletteOf('standard', false, token)

        expect(palette.lt1).toBe(token.colorBgContainer)
        expect(palette.accent1).toBe(token.colorPrimary)
        // The text of the theme at its full strength, as the ink of Excel is black.
        expect(palette.dk1).toBe('#000000')
        expect(palette).toMatchObject(THEMES.standard.light.accents)
        expect(paletteOf('dracula', true)).toMatchObject({ lt1: '#282a36', ...THEMES.dracula.dark.accents })
    })

    it('makes a theme colour lighter towards the ground of the palette and darker towards its text', () => {
        const palette = paletteOf('standard', false)

        expect(colourIn(palette, { name: 'accent1' })).toBe(palette.accent1)
        expect(colourIn(palette, BLUE_TITLE)).toBe(mix(palette.accent1, palette.lt1, 0.6))
        expect(colourIn(palette, { name: 'accent6', tint: -0.25 })).toBe(mix(palette.accent6, palette.dk1, 0.25))
        // A colour worked out once is handed out the same.
        expect(colourIn(palette, BLUE_TITLE)).toBe(colourIn(palette, BLUE_TITLE))
    })

    it('keeps a lighter fill quiet in the dark appearance: closer to the dark ground than its theme colour', () => {
        const dark = paletteOf('standard', true)
        const fill = colourIn(dark, VALUES)

        expect(contrastRatio(fill, dark.lt1)).toBeLessThan(contrastRatio(dark.accent5, dark.lt1))
        expect(contrastRatio(fill, dark.dk1)).toBeGreaterThan(contrastRatio(dark.accent5, dark.dk1))
    })

    /** How far the text of a table stands from each fill of the formatting standard, and the muted text from the ground. */
    const contrastsOf = (name: ThemeName, isDarkMode: boolean) => {
        const palette = paletteOf(name, isDarkMode)
        const fills = [{ name: 'lt1' } as RawTableThemeColor, GREY_TITLE, BLUE_TITLE, VALUES]
        return {
            text: Math.min(...fills.map(fill => contrastRatio(palette.dk1, colourIn(palette, fill)))),
            muted: contrastRatio(colourIn(palette, MUTED), palette.lt1),
        }
    }

    it.each([false, true])('keeps the tables of the standard theme as readable as WCAG AA asks, dark %s', isDarkMode => {
        const { text, muted } = contrastsOf('standard', isDarkMode)

        expect(text).toBeGreaterThanOrEqual(4.5)
        expect(muted).toBeGreaterThanOrEqual(3)
    })

    // A code editor softens its text on its background, Material's dark one down to 7:1, which a fill between them
    // shares: the text keeps more than the 3:1 WCAG AA asks of large text.
    it.each(LOOKS)('keeps the text of a table readable on every fill of the formatting standard in %s, dark %s',
        (name, isDarkMode) => {
            const { text, muted } = contrastsOf(name, isDarkMode)

            expect(text).toBeGreaterThanOrEqual(3.5)
            expect(muted).toBeGreaterThanOrEqual(2.3)
        })

    it('draws every colour of a cell in the colours of the palette, and a colour it has none for as it is', () => {
        const table = tableThemeOf('dracula', true, tokenFor('dracula', true), false)
        const palette = table.palette as ExcelPalette
        const rows: RawTableCell[][] = [[
            {
                cell: 'B2',
                style: {
                    background: '#b4c6e7',
                    backgroundTheme: BLUE_TITLE,
                    color: '#ff0000',
                    border: {
                        top: { style: 'solid', width: 1, color: '#808080', colorTheme: MUTED },
                        bottom: { style: 'solid', width: 1, color: '#123456' },
                    },
                },
                runs: [{ text: 'Data', style: { color: '#808080', colorTheme: MUTED } }, { text: 'type' }],
            },
            { cell: 'C2', value: 'plain' },
        ]]

        const [painted, plain] = paintedIn(rows, table)[0] ?? []

        expect(painted?.style?.background).toBe(colourIn(palette, BLUE_TITLE))
        expect(painted?.style?.color, 'A colour the server tells no colour of the palette for').toBe('#ff0000')
        expect(painted?.style?.border?.top?.color).toBe(colourIn(palette, MUTED))
        expect(painted?.style?.border?.bottom?.color).toBe('#123456')
        expect(painted?.runs?.[0]?.style?.color).toBe(colourIn(palette, MUTED))
        expect(painted?.runs?.[1]).toBe(rows[0]?.[0]?.runs?.[1])
        expect(plain).toBe(rows[0]?.[1])
    })

    it('lays the tables on the colours of the theme', () => {
        const token = tokenFor('nord', true)
        const table = tableThemeOf('nord', true, token, false)

        expect(table.paper.background).toBe(table.palette?.lt1)
        expect(table.paper.text).toBe(table.palette?.dk1)
        expect(table.paper.note).toBe(token.colorError)
        // The marks a screen lays on the cells are the theme's own.
        expect(table.marks).toBe(token)
    })

    it('keeps the colours of the Excel file, on the paper of a workbook, where the reader asks for them', () => {
        const table = tableThemeOf('dracula', true, tokenFor('dracula', true), true)
        const rows: RawTableCell[][] = [[{ cell: 'B2', style: { background: '#b4c6e7', backgroundTheme: BLUE_TITLE } }]]

        expect(table.palette).toBeUndefined()
        expect(table.paper).toEqual(workbookPaper())
        expect(table.marks).toBe(paperToken())
        expect(paintedIn(rows, table)).toBe(rows)
    })

    it('draws the tables of a part drawn without the theme provider in the colours of the standard theme', () => {
        const token = tokenFor('standard', false)
        const provided = tableThemeOf('nord', false, tokenFor('nord', false), false)

        expect(tableOf({ ...token, table: provided })).toBe(provided)
        expect(tableOf(token).palette).toEqual(excelPaletteOf('standard', false, token))
        expect(tableOf(token), 'Worked out once for a token').toBe(tableOf(token))
    })
})
