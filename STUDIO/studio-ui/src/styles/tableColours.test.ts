import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { parse } from 'yaml'
import { contrastRatio, opaque } from './colorMath'
import { followedTableTheme, inLook, LOOK_THEMES, tableColoursOf, type TableColours } from './tableColours'
import type { ThemeName } from './themes'
import type { RawTableCell } from 'types/tables'
import { tokenFor } from '../testing/theme'

/** The colours of the look a theme of the application gives its tables in the dark appearance. */
const darkColoursOf = (name: ThemeName): TableColours => {
    const colours = tableColoursOf(name, true, tokenFor(name, true))
    if (colours === undefined) {
        throw new Error(`The ${name} theme has no look for the tables`)
    }
    return colours
}

/** The colour the colours give a key, which the test needs to be there. */
const keyed = (colours: TableColours, key: string): string => {
    const colour = colours.keyed[key]
    if (colour === undefined) {
        throw new Error(`No colour for ${key}`)
    }
    return colour
}

/**
 * Every key a table theme file of the server sets a colour at, as the server reads the file: with its aliases and
 * merge keys resolved, and without the colours it names and the theme colours of Excel, which no part takes as they
 * are.
 */
const colourKeysOf = (tableTheme: string): string[] => {
    const file = resolve(process.cwd(), '../org.openl.rules.webstudio/resources/table-themes', `${tableTheme}.yaml`)
    const { colors: _named, themeColors: _excel, ...theme } = parse(readFileSync(file, 'utf8'), { merge: true }) as
        Record<string, unknown>
    const walk = (node: unknown, prefix: string): string[] => (node !== null && typeof node === 'object'
        ? Object.entries(node).flatMap(([key, value]) => (key === 'color' || key === 'background'
            ? [`${prefix}${key}`]
            : walk(value, `${prefix}${key}.`)))
        : [])
    return walk(theme, '').sort()
}

// The keys of the Standard table theme a text, a fill and a link lie at: the ink and the ground of its base, the
// muted text, the grey and the blue titles, the fills of what a table gives, and of the technical tables.
const INK = 'base.style.color'
const MUTED = 'base.type.color'
const GROUND = 'base.style.background'
const GREY = 'base.titles.background'
const BLUE = 'base.returnTitles.background'
const LIGHT_BLUE = 'base.returns.background'
const LIGHT_GREY = 'environment.header.style.background'
const PALE_GREY = 'environment.name.background'
const FILLS = [GROUND, GREY, BLUE, LIGHT_BLUE, LIGHT_GREY, PALE_GREY]
const LINK_FILLS = [GROUND, LIGHT_BLUE, LIGHT_GREY, PALE_GREY]

describe('tableColours', () => {
    it('reads the tables with the table theme of the look of the Studio theme, where the reader asks for it', () => {
        expect(followedTableTheme(true, 'standard')).toBe('standard')
        // The table theme of the settings is the fallback: for a reader who does not ask, and under a theme with no
        // look of its own for the tables.
        expect(followedTableTheme(false, 'standard')).toBeUndefined()
        expect(followedTableTheme(true, 'dracula')).toBeUndefined()
    })

    it.each(LOOK_THEMES)('colours in the dark every key the table theme file of %s sets a colour at', name => {
        const tableTheme = followedTableTheme(true, name) ?? name

        expect(Object.keys(darkColoursOf(name).keyed).sort()).toEqual(colourKeysOf(tableTheme))
    })

    it('keeps the colours of the table theme in the light appearance, laid on the ground of the theme', () => {
        const token = tokenFor('standard', false)
        const solid = (colour: string) => opaque(colour, token.colorBgContainer)

        expect(tableColoursOf('standard', false, token)).toEqual({
            keyed: {},
            paper: {
                background: solid(token.colorBgContainer),
                text: solid(token.colorText),
                grid: solid(token.colorBorderSecondary),
                link: solid(token.colorLink),
                linkHover: solid(token.colorLinkHover),
                note: solid(token.colorError),
            },
        })
    })

    it('colours a key in a solid colour of the dark token, and lays the table on its base style', () => {
        const token = tokenFor('standard', true)

        const colours = darkColoursOf('standard')

        expect(keyed(colours, 'datatype.name.background')).toBe(opaque(token.colorPrimaryBg, token.colorBgContainer))
        // A translucent fill of the token is given as it shows on the ground of the table.
        expect(keyed(colours, GREY)).toBe(opaque(token.colorFill, token.colorBgContainer))
        expect(keyed(colours, GREY)).toMatch(/^#[0-9a-f]{6}$/)
        // The ground and the ink of the table are the colours of the base style.
        expect(colours.paper.background).toBe(keyed(colours, GROUND))
        expect(colours.paper.text).toBe(keyed(colours, INK))
        // A part an alias repeats in the file takes the colour of the part it repeats.
        expect(keyed(colours, 'tbasic.values.background')).toBe(keyed(colours, 'spreadsheet.values.background'))
    })

    it('keeps the greys and the blues as far from the ground as the table theme has them', () => {
        const colours = darkColoursOf('standard')
        const fromGround = (key: string) => contrastRatio(keyed(colours, key), colours.paper.background)

        expect(fromGround(GREY)).toBeGreaterThan(fromGround(LIGHT_GREY))
        expect(fromGround(LIGHT_GREY)).toBeGreaterThan(fromGround(PALE_GREY))
        expect(fromGround(BLUE)).toBeGreaterThan(fromGround(LIGHT_BLUE))
    })

    it.each(LOOK_THEMES)('keeps every text of a table readable on every fill of %s in the dark', name => {
        const colours = darkColoursOf(name)

        FILLS.forEach(fill => {
            expect(contrastRatio(keyed(colours, INK), keyed(colours, fill))).toBeGreaterThanOrEqual(4.5)
            expect(contrastRatio(keyed(colours, MUTED), keyed(colours, fill))).toBeGreaterThanOrEqual(3)
        })
    })

    it.each(LOOK_THEMES)('keeps a link of a table legible on the fills it lies on, in %s in the dark', name => {
        const colours = darkColoursOf(name)

        LINK_FILLS.forEach(fill => expect(contrastRatio(colours.paper.link, keyed(colours, fill)))
            .toBeGreaterThanOrEqual(3))
    })

    it('draws a link of a dark table in the lighter text of the primary colour, under the pointer as well', () => {
        const token = tokenFor('standard', true)

        const colours = darkColoursOf('standard')

        expect(colours.paper.link).toBe(opaque(token.colorPrimaryTextHover, token.colorBgContainer))
        // The link of the token darkens under the pointer; the underline alone tells a link under the pointer here.
        expect(colours.paper.linkHover).toBe(colours.paper.link)
    })

    it('has no colours for the tables under a theme with no look of its own for them', () => {
        expect(tableColoursOf('dracula', true, tokenFor('dracula', true))).toBeUndefined()
        expect(tableColoursOf('dracula', false, tokenFor('dracula', false))).toBeUndefined()
    })

    it('recolours only what the table theme draws a cell with, and leaves the style of the workbook as it is', () => {
        const colours = darkColoursOf('standard')
        const plain: RawTableCell = { cell: 'A1', value: 'Notes', style: { background: '#ffff00' } }
        const themed: RawTableCell = {
            cell: 'B1',
            value: 'name',
            style: { background: '#ddebf7', backgroundKey: 'datatype.name.background', fontSize: 10, source: 'theme' },
            runs: [{ text: 'name', style: { color: '#808080', colorKey: 'base.type.color', source: 'theme' } }],
        }

        const [drawnPlain, drawnThemed] = inLook([[plain, themed]], colours)[0] ?? []

        expect(drawnPlain).toBe(plain)
        // The text takes the font of the application.
        expect(drawnThemed?.style).toEqual({
            background: keyed(colours, 'datatype.name.background'),
            backgroundKey: 'datatype.name.background',
            source: 'theme',
        })
        expect(drawnThemed?.runs?.[0]?.style?.color).toBe(keyed(colours, 'base.type.color'))
    })
})
