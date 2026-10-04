import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { contrastRatio, opaque } from './colorMath'
import { LIST_PAGE_COLORS, paletteVariables } from './listPageTheme'
import { paletteFor } from '../testing/theme'
import { THEME_ORDER } from './themes'

const VARIANTS = THEME_ORDER.flatMap(name => [false, true].map(isDarkMode => ({
    name,
    appearance: isDarkMode ? 'dark' : 'light',
    palette: paletteFor(name, isDarkMode),
})))

/** The contrast of a colour, translucent or not, against the surface it is drawn on. */
const contrastOn = (color: string, surface: string): number => contrastRatio(opaque(color, surface), surface)

describe('listPageTheme', () => {
    it('paints the standard theme in the colours of Ant Design', () => {
        const light = antdTheme.getDesignToken()
        const dark = antdTheme.getDesignToken({ algorithm: antdTheme.darkAlgorithm })

        expect(paletteFor('standard', false)).toMatchObject({
            primary: '#1677ff',
            pageBg: light.colorBgLayout,
            containerBg: light.colorBgContainer,
            text: light.colorText,
            border: light.colorBorder,
        })
        expect(paletteFor('standard', true)).toMatchObject({
            primary: dark.colorPrimary,
            pageBg: dark.colorBgLayout,
            containerBg: dark.colorBgContainer,
            text: dark.colorText,
            border: dark.colorBorder,
        })
    })

    it('lays the containers of a theme drawn from a code editor on the background of the editor', () => {
        expect(paletteFor('dracula', true).containerBg).toBe('#282a36')
        expect(paletteFor('dracula', false).containerBg).toBe('#fffbeb')
        expect(paletteFor('solarized', false).pageBg).not.toBe(paletteFor('solarized', false).containerBg)
    })

    it('reads the palette of the theme and the appearance asked for', () => {
        expect(paletteFor('nord', true).primary).not.toBe(paletteFor('nord', false).primary)
        expect(paletteFor('nord', true).primary).not.toBe(paletteFor('standard', true).primary)
    })

    it.each(VARIANTS)('keeps the text of $name $appearance readable on every surface', ({ palette }) => {
        [palette.containerBg, palette.pageBg].forEach(surface => {
            expect(contrastOn(palette.text, surface)).toBeGreaterThanOrEqual(4.5)
        })
        expect(contrastOn(palette.textSecondary, palette.containerBg)).toBeGreaterThanOrEqual(3)
    })

    it.each(VARIANTS)('keeps the links and the primary buttons of $name $appearance legible', ({ palette }) => {
        expect(contrastRatio(palette.primary, palette.containerBg)).toBeGreaterThanOrEqual(3)
        expect(contrastRatio(palette.primaryFg, palette.primary)).toBeGreaterThanOrEqual(3)
    })

    it.each(VARIANTS)('fills the solid badges of $name $appearance deep enough for white text', ({ palette }) => {
        const fills = [palette.statusNeutral, palette.statusRunning, palette.statusPaused, palette.statusFinished,
            palette.statusFailed]

        fills.forEach(fill => expect(contrastRatio('#ffffff', fill)).toBeGreaterThanOrEqual(4.5))
    })

    it('colours a parameter value in the hues the code editor of the theme gives a value', () => {
        expect(paletteFor('monokai', true)).toMatchObject({
            syntaxName: '#66d9ef',
            syntaxString: '#e6db74',
            syntaxNumber: '#ae81ff',
            syntaxBoolean: '#fd971f',
        })
        // Dracula writes numbers in its plain text colour, and so does the value.
        expect(paletteFor('dracula', true).syntaxNumber).toBe('#f8f8f2')
        // So does CodeMirror's own light theme a property name, whose plain text is the text of the page.
        expect(paletteFor('standard', false).syntaxName).toBe(paletteFor('standard', false).text)
    })

    it('hands styles a custom property rather than a fixed colour', () => {
        expect(LIST_PAGE_COLORS.primary).toBe('var(--openl-primary)')
        expect(LIST_PAGE_COLORS.textSecondary).toBe('var(--openl-text-secondary)')
    })

    it('publishes the palette of the appearance it is given', () => {
        const dark = paletteFor('standard', true)
        const light = paletteFor('standard', false)

        expect(paletteVariables(dark)).toContain(`--openl-primary: ${dark.primary};`)
        expect(paletteVariables(dark)).toContain(`--openl-page-bg: ${dark.pageBg};`)
        expect(paletteVariables(light)).toContain(`--openl-page-bg: ${light.pageBg};`)
    })
})
