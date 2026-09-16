import { describe, expect, it } from 'vitest'
import { DARK_PALETTE, LIGHT_PALETTE, LIST_PAGE_COLORS, paletteVariables } from './listPageTheme'

describe('listPageTheme', () => {
    it('gives every colour a counterpart in the other appearance', () => {
        expect(Object.keys(DARK_PALETTE).sort()).toEqual(Object.keys(LIGHT_PALETTE).sort())
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
