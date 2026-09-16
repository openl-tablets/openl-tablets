import { describe, expect, it } from 'vitest'
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

    it('keeps the same shape in both appearances', () => {
        expect(projectsTheme(true).token?.borderRadius).toBe(projectsTheme(false).token?.borderRadius)
        expect(projectsTheme(true).components?.Button?.controlHeight)
            .toBe(projectsTheme(false).components?.Button?.controlHeight)
    })
})
