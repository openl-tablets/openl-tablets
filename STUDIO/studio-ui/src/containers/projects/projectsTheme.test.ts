import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { projectsTheme } from './projectsTheme'
import { paletteFor, tokenFor } from '../../testing/theme'
import type { ThemeName } from '../../styles/themes'

/** The scoped theme of a theme in an appearance, worked out from its token and its palette. */
const themed = (name: ThemeName, isDarkMode: boolean) => projectsTheme(tokenFor(name, isDarkMode), paletteFor(name, isDarkMode))

const LIGHT = themed('standard', false)
const DARK = themed('standard', true)

describe('projectsTheme', () => {
    it('takes its colours from the theme of the application, naming none of the shared ones itself', () => {
        const { token } = LIGHT

        Object.keys(token ?? {}).forEach(name => expect(name).not.toMatch(/^color/))
    })

    it('draws the tabs and the segmented controls of the mockup in the token and the palette it is handed', () => {
        const { components } = DARK
        const token = tokenFor('standard', true)

        expect(components?.Tabs).toMatchObject({ itemColor: token.colorTextTertiary, itemSelectedColor: token.colorText })
        expect(components?.Segmented?.trackBg).toBe(paletteFor('standard', true).secondaryBg)
    })

    it('follows the picked theme, not only the appearance', () => {
        const dracula = themed('dracula', false)

        expect(dracula.components?.Tabs?.itemColor).toBe(tokenFor('dracula', false).colorTextTertiary)
        expect(dracula.components?.Tabs?.itemColor).not.toBe(LIGHT.components?.Tabs?.itemColor)
    })

    it('hands Ant Design real colours, never a custom property it cannot derive a palette from', () => {
        expect(JSON.stringify(DARK)).not.toContain('var(--')
    })

    it('states its measurements as seed tokens, so a density can still scale them', () => {
        const config = LIGHT

        // A measurement named under `components` is put back verbatim after the algorithm has run, so it
        // would stand at its comfortable size on a compact screen. Only the seed may carry one.
        const componentSizes = JSON.stringify(config.components ?? {})

        expect(componentSizes).not.toContain('controlHeight')
        expect(componentSizes).not.toContain('fontSize')
        expect(config.token?.controlHeight).toBe(36)
    })

    it('tightens its controls and its type at the compact density', () => {
        const config = LIGHT
        const comfortable = antdTheme.getDesignToken(config)
        const compact = antdTheme.getDesignToken({ ...config, algorithm: antdTheme.compactAlgorithm })

        expect(compact.controlHeight).toBeLessThan(comfortable.controlHeight)
        expect(compact.fontSize).toBeLessThan(comfortable.fontSize)
        expect(compact.paddingSM).toBeLessThan(comfortable.paddingSM)
        // The mockup's 4px lift over the Ant Design height survives in either density.
        expect(comfortable.controlHeight).toBe(36)
        expect(compact.controlHeight).toBe(32)
    })

    it('keeps the same shape under every palette', () => {
        expect(DARK.token).toEqual(LIGHT.token)
        expect(themed('kimbie', true).token).toEqual(LIGHT.token)
    })
})
