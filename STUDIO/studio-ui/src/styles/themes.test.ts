import { describe, expect, it } from 'vitest'
import { accentOf, codeBlockColors, THEME_ORDER, THEMES, variantOf } from './themes'

describe('themes', () => {
    it('offers fourteen themes, the standard one first', () => {
        expect(THEME_ORDER[0]).toBe('standard')
        expect(THEME_ORDER).toHaveLength(14)
    })

    it('picks the variant of the appearance asked for', () => {
        expect(variantOf('dracula', true).editor?.background).toBe('#282a36')
        expect(variantOf('dracula', false).editor?.background).toBe('#fffbeb')
    })

    it('draws every theme but the standard one from a code editor', () => {
        THEME_ORDER.forEach(name => [false, true].forEach(isDarkMode => {
            expect(variantOf(name, isDarkMode).editor === undefined).toBe(name === 'standard')
        }))
    })

    it('shows the accent of the variant in force, and Ant Design\'s own for the standard theme', () => {
        expect(accentOf('github', false)).toBe('#0969da')
        expect(accentOf('github', true)).toBe('#58a6ff')
        expect(accentOf('standard', true)).toBe('#1677ff')
    })

    it('draws a block of code in the background and the text of the code editor', () => {
        expect(codeBlockColors('nord', true)).toEqual({ background: '#2e3440', color: '#ffffff' })
        expect(codeBlockColors('standard', true)).toEqual({})
    })

    it('writes every colour out in full', () => {
        const colours = JSON.stringify(THEMES).match(/"#[^"]*"/g) ?? []

        expect(colours.length).toBeGreaterThan(0)
        colours.forEach(colour => expect(colour).toMatch(/^"#[0-9a-f]{6}"$/))
    })
})
