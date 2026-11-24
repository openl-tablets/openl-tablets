import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { contrastRatio, opaque } from './colorMath'
import { LIST_PAGE_COLORS, paletteVariables } from './listPageTheme'
import { paletteFor, tokenFor } from '../testing/theme'
import { THEME_ORDER } from './themes'

const VARIANTS = THEME_ORDER.flatMap(name => [false, true].map(isDarkMode => ({
    name,
    appearance: isDarkMode ? 'dark' : 'light',
    palette: paletteFor(name, isDarkMode),
    token: tokenFor(name, isDarkMode),
})))

/** The contrast of a colour, translucent or not, against the surface it is drawn on. */
const contrastOn = (color: string, surface: string): number => contrastRatio(opaque(color, surface), surface)

describe('listPageTheme', () => {
    it('paints the standard theme in the colours of Ant Design', () => {
        const light = antdTheme.getDesignToken()
        const dark = antdTheme.getDesignToken({ algorithm: antdTheme.darkAlgorithm })

        expect(tokenFor('standard', false)).toMatchObject({
            colorPrimary: '#1677ff',
            colorBgLayout: light.colorBgLayout,
            colorBgContainer: light.colorBgContainer,
            colorText: light.colorText,
            colorBorder: light.colorBorder,
        })
        expect(tokenFor('standard', true)).toMatchObject({
            colorPrimary: dark.colorPrimary,
            colorBgLayout: dark.colorBgLayout,
            colorBgContainer: dark.colorBgContainer,
            colorText: dark.colorText,
            colorBorder: dark.colorBorder,
        })
        // The brand is the primary one step towards the text: the shade Ant Design presses a primary in.
        expect(paletteFor('standard', false).brand).toBe(light.colorPrimaryActive)
        expect(paletteFor('standard', true).brand).toBe(dark.colorPrimaryHover)
    })

    it('lays the containers of a theme drawn from a code editor on the background of the editor', () => {
        expect(tokenFor('dracula', true).colorBgContainer).toBe('#282a36')
        expect(tokenFor('dracula', false).colorBgContainer).toBe('#fffbeb')
        expect(tokenFor('solarized', false).colorBgLayout).not.toBe(tokenFor('solarized', false).colorBgContainer)
    })

    it('reads the palette of the theme and the appearance asked for', () => {
        expect(paletteFor('nord', true).brand).not.toBe(paletteFor('nord', false).brand)
        expect(paletteFor('nord', true).brand).not.toBe(paletteFor('standard', true).brand)
    })

    it.each(VARIANTS)('keeps the text of $name $appearance readable on every surface', ({ token }) => {
        [token.colorBgContainer, token.colorBgLayout].forEach(surface => {
            expect(contrastOn(token.colorText, surface)).toBeGreaterThanOrEqual(4.5)
        })
        expect(contrastOn(token.colorTextSecondary, token.colorBgContainer)).toBeGreaterThanOrEqual(3)
    })

    it.each(VARIANTS)('keeps the links and the primary buttons of $name $appearance legible', ({ token }) => {
        expect(contrastRatio(token.colorPrimary, token.colorBgContainer)).toBeGreaterThanOrEqual(3)
        expect(contrastRatio(token.colorTextLightSolid, token.colorPrimary)).toBeGreaterThanOrEqual(3)
    })

    it.each(VARIANTS)('fills the solid badges of $name $appearance deep enough for white text', ({ palette }) => {
        const fills = [
            palette.statusNeutral,
            palette.statusRunning,
            palette.statusPaused,
            palette.statusFinished,
            palette.statusFailed
        ]

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
        expect(paletteFor('standard', false).syntaxName).toBe(tokenFor('standard', false).colorText)
    })

    it('hands styles a custom property rather than a fixed colour', () => {
        expect(LIST_PAGE_COLORS.brand).toBe('var(--openl-brand)')
        expect(LIST_PAGE_COLORS.secondaryBg).toBe('var(--openl-secondary-bg)')
    })

    it('publishes the palette of the appearance it is given', () => {
        const dark = paletteFor('standard', true)
        const light = paletteFor('standard', false)

        expect(paletteVariables(dark)).toContain(`--openl-brand: ${dark.brand};`)
        expect(paletteVariables(dark)).toContain(`--openl-secondary-bg: ${dark.secondaryBg};`)
        expect(paletteVariables(light)).toContain(`--openl-secondary-bg: ${light.secondaryBg};`)
        // A colour Ant Design has a token for is not published again.
        expect(paletteVariables(light)).not.toContain('--openl-page-bg')
    })
})
