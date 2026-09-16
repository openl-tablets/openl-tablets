import { describe, expect, it } from 'vitest'
import {
    DARK_PALETTE,
    LIGHT_PALETTE,
    LIST_PAGE_COLORS,
    paletteOf,
    paletteVariables,
    THEME_ORDER,
    THEMES,
} from './listPageTheme'

describe('listPageTheme', () => {
    it('gives every colour a counterpart in the other appearance, under every theme', () => {
        const keys = Object.keys(LIGHT_PALETTE).sort()

        Object.values(THEMES).forEach(({ dark, light }) => {
            expect(Object.keys(light).sort()).toEqual(keys)
            expect(Object.keys(dark).sort()).toEqual(keys)
        })
    })

    it('offers the standard theme first, and every theme it knows', () => {
        expect(THEME_ORDER[0]).toBe('standard')
        expect([...THEME_ORDER].sort()).toEqual(Object.keys(THEMES).sort())
    })

    it('hands out the palette of the theme and the appearance asked for', () => {
        expect(paletteOf('standard', false)).toBe(LIGHT_PALETTE)
        expect(paletteOf('standard', true)).toBe(DARK_PALETTE)
        expect(paletteOf('evergreen', false).primary).not.toBe(LIGHT_PALETTE.primary)
    })

    it('writes every colour out, so no theme borrows one from another', () => {
        Object.values(THEMES).forEach(({ dark, light }) => {
            expect(Object.values(light).every(colour => /^#[0-9a-f]{6}$/.test(colour))).toBe(true)
            expect(Object.values(dark).every(colour => /^#[0-9a-f]{6}$/.test(colour))).toBe(true)
        })
    })

    it('hands styles a custom property rather than a fixed colour', () => {
        expect(LIST_PAGE_COLORS.primary).toBe('var(--openl-primary)')
        expect(LIST_PAGE_COLORS.textSecondary).toBe('var(--openl-text-secondary)')
    })

    it('publishes the palette of the appearance it is given', () => {
        const declarations = paletteVariables(DARK_PALETTE)

        expect(declarations).toContain(`--openl-primary: ${DARK_PALETTE.primary};`)
        expect(declarations).toContain(`--openl-page-bg: ${DARK_PALETTE.pageBg};`)
        expect(paletteVariables(LIGHT_PALETTE)).toContain(`--openl-page-bg: ${LIGHT_PALETTE.pageBg};`)
    })
})
