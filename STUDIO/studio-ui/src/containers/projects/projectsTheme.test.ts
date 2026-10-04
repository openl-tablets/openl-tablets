import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { projectsTheme } from './projectsTheme'
import { paletteFor } from '../../testing/theme'

const LIGHT = paletteFor('standard', false)
const DARK = paletteFor('standard', true)

describe('projectsTheme', () => {
    it('takes its colours from the theme of the application, naming none of the shared ones itself', () => {
        const { token } = projectsTheme(LIGHT)

        Object.keys(token ?? {}).forEach(name => expect(name).not.toMatch(/^color/))
    })

    it('draws the tabs and the segmented controls of the mockup in the palette it is handed', () => {
        const { components } = projectsTheme(DARK)

        expect(components?.Tabs).toMatchObject({ itemColor: DARK.textTertiary, itemSelectedColor: DARK.text })
        expect(components?.Segmented?.trackBg).toBe(DARK.secondaryBg)
    })

    it('follows the picked theme, not only the appearance', () => {
        const dracula = paletteFor('dracula', false)

        expect(projectsTheme(dracula).components?.Tabs?.itemColor).toBe(dracula.textTertiary)
        expect(projectsTheme(dracula).components?.Tabs?.itemColor)
            .not.toBe(projectsTheme(LIGHT).components?.Tabs?.itemColor)
    })

    it('hands Ant Design real colours, never a custom property it cannot derive a palette from', () => {
        expect(JSON.stringify(projectsTheme(DARK))).not.toContain('var(--')
    })

    it('states its measurements as seed tokens, so a density can still scale them', () => {
        const config = projectsTheme(LIGHT)

        // A measurement named under `components` is put back verbatim after the algorithm has run, so it
        // would stand at its comfortable size on a compact screen. Only the seed may carry one.
        const componentSizes = JSON.stringify(config.components ?? {})

        expect(componentSizes).not.toContain('controlHeight')
        expect(componentSizes).not.toContain('fontSize')
        expect(config.token?.controlHeight).toBe(36)
    })

    it('tightens its controls and its type at the compact density', () => {
        const config = projectsTheme(LIGHT)
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
        expect(projectsTheme(DARK).token).toEqual(projectsTheme(LIGHT).token)
        expect(projectsTheme(paletteFor('kimbie', true)).token).toEqual(projectsTheme(LIGHT).token)
    })
})
