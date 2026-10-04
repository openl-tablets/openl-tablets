import { describe, expect, it } from 'vitest'
import { FastColor } from '@ant-design/fast-color'
import { appTheme } from './appTheme'

const luminance = (color: unknown): number => new FastColor(String(color)).getLuminance()

describe('appTheme', () => {
    it('leaves the standard theme to Ant Design', () => {
        expect(appTheme('standard', false)).toEqual({})
        expect(appTheme('standard', true)).toEqual({})
    })

    it('hands Ant Design the background, the text and the accent of the code editor', () => {
        expect(appTheme('dracula', true).token).toMatchObject({
            colorBgBase: '#282a36',
            colorBgContainer: '#282a36',
            colorTextBase: '#f8f8f2',
            colorPrimary: '#bd93f9',
            colorLink: '#bd93f9',
        })
    })

    it('paints the text of the screens in the colour the theme names for them, where the editor names its own', () => {
        expect(appTheme('vscode', true).token?.colorTextBase).toBe('#d4d4d4')
    })

    it('lays the page a shade darker than the containers in either appearance', () => {
        [false, true].forEach(isDarkMode => {
            const token = appTheme('gruvbox', isDarkMode).token

            expect(luminance(token?.colorBgLayout)).toBeLessThan(luminance(token?.colorBgContainer))
        })
    })

    it('lifts what floats above the page in the dark appearance, and draws the borders lighter still', () => {
        const token = appTheme('nord', true).token

        expect(luminance(token?.colorBgElevated)).toBeGreaterThan(luminance(token?.colorBgContainer))
        expect(luminance(token?.colorBorder)).toBeGreaterThan(luminance(token?.colorBorderSecondary))
    })

    it('keeps the tint of the background of the editor in the surfaces around it', () => {
        const token = appTheme('solarized', false).token
        const background = new FastColor(String(token?.colorBgContainer))
        const page = new FastColor(String(token?.colorBgLayout))

        // Ant Design's own derivation turns this cream page into a grey of under 0.02.
        expect(page.getSaturation()).toBeGreaterThan(background.getSaturation() / 2)
        expect(Math.abs(page.getHue() - background.getHue())).toBeLessThan(10)
    })

    it('hands Ant Design real colours, never a custom property it cannot derive a palette from', () => {
        expect(JSON.stringify(appTheme('material', true))).not.toContain('var(--')
    })
})
