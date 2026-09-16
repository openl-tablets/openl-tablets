import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { projectsTheme } from './projectsTheme'
import { DARK_PALETTE, LIGHT_PALETTE } from '../../styles/listPageTheme'

describe('projectsTheme', () => {
    it('paints the Projects screens in the palette of the appearance in force', () => {
        expect(projectsTheme(false).token?.colorText).toBe(LIGHT_PALETTE.text)
        expect(projectsTheme(false).token?.colorBgLayout).toBe(LIGHT_PALETTE.pageBg)
        expect(projectsTheme(true).token?.colorText).toBe(DARK_PALETTE.text)
        expect(projectsTheme(true).token?.colorBgLayout).toBe(DARK_PALETTE.pageBg)
    })

    it('hands Ant Design real colours, never a custom property it cannot derive a palette from', () => {
        const { token } = projectsTheme(true)

        expect(token?.colorPrimary).toBe(DARK_PALETTE.primary)
        expect(JSON.stringify(token)).not.toContain('var(--')
    })

    it('states its measurements as seed tokens, so a density can still scale them', () => {
        const config = projectsTheme(false)

        // A measurement named under `components` is put back verbatim after the algorithm has run, so it
        // would stand at its comfortable size on a compact screen. Only the seed may carry one.
        const componentSizes = JSON.stringify(config.components ?? {})

        expect(componentSizes).not.toContain('controlHeight')
        expect(componentSizes).not.toContain('fontSize')
        expect(config.token?.controlHeight).toBe(36)
    })

    it('tightens its controls and its type at the compact density', () => {
        const config = projectsTheme(false)
        const comfortable = antdTheme.getDesignToken(config)
        const compact = antdTheme.getDesignToken({ ...config, algorithm: antdTheme.compactAlgorithm })

        expect(compact.controlHeight).toBeLessThan(comfortable.controlHeight)
        expect(compact.fontSize).toBeLessThan(comfortable.fontSize)
        expect(compact.paddingSM).toBeLessThan(comfortable.paddingSM)
        // The mockup's 4px lift over the Ant Design height survives in either density.
        expect(comfortable.controlHeight).toBe(36)
        expect(compact.controlHeight).toBe(32)
    })

    it('keeps the same shape in both appearances', () => {
        expect(projectsTheme(true).token?.borderRadius).toBe(projectsTheme(false).token?.borderRadius)
        expect(projectsTheme(true).token?.controlHeight).toBe(projectsTheme(false).token?.controlHeight)
    })
})
