import { cleanup, render, screen } from '@testing-library/react'
import { createStyles } from 'antd-style'
import { theme as antdTheme } from 'antd'
import { beforeEach, describe, expect, it } from 'vitest'
import { ProjectsThemeProvider } from './ProjectsThemeProvider'
import { AppThemeProvider } from '../../providers/AppThemeProvider'
import { DARK_PALETTE, LIGHT_PALETTE } from '../../styles/listPageTheme'
import { THEME_COMPACT_KEY, THEME_MODE_KEY } from '../../utils/themeMode'

// A co-located style, the way every Projects component writes one — it must see the scoped token.
const useProbeStyles = createStyles(({ css, token }) => ({
    probe: css`background: ${token.colorBgLayout};`,
}))

const StyleProbe = () => {
    const { theme } = useProbeStyles()

    return (
        <>
            <span data-testid="page-bg">{theme.colorBgLayout}</span>
            <span data-testid="padding">{theme.padding}</span>
            {/* Named by no theme of ours, so it can only come from the appearance algorithm. */}
            <span data-testid="elevated">{theme.colorBgElevated}</span>
        </>
    )
}

const renderScoped = () => render(
    <AppThemeProvider>
        <ProjectsThemeProvider>
            <span data-testid="projects-child">Projects</span>
            <StyleProbe />
        </ProjectsThemeProvider>
    </AppThemeProvider>
)

describe('ProjectsThemeProvider', () => {
    beforeEach(() => localStorage.clear())

    it('renders children inside the scoped projects theme', () => {
        renderScoped()

        expect(screen.getByTestId('projects-child').textContent).toBe('Projects')
    })

    it('hands co-located styles the scoped token, not the application-wide one', () => {
        renderScoped()

        expect(screen.getByTestId('page-bg').textContent).toBe(LIGHT_PALETTE.pageBg)
    })

    it('turns dark with the rest of the application', () => {
        localStorage.setItem(THEME_MODE_KEY, 'dark')

        renderScoped()

        expect(screen.getByTestId('page-bg').textContent).toBe(DARK_PALETTE.pageBg)
    })

    it('stays dark at the compact density, because a density is added to the appearance and not put in its place', () => {
        localStorage.setItem(THEME_MODE_KEY, 'dark')
        localStorage.setItem(THEME_COMPACT_KEY, 'true')
        const dark = antdTheme.getDesignToken({ algorithm: antdTheme.darkAlgorithm })

        renderScoped()

        expect(screen.getByTestId('elevated').textContent).toBe(dark.colorBgElevated)
        expect(screen.getByTestId('page-bg').textContent).toBe(DARK_PALETTE.pageBg)
        expect(Number(screen.getByTestId('padding').textContent)).toBeLessThan(dark.padding)
    })

    it('tightens with the rest of the application, keeping its own colours', () => {
        renderScoped()
        const comfortable = Number(screen.getByTestId('padding').textContent)
        cleanup()
        localStorage.setItem(THEME_COMPACT_KEY, 'true')

        renderScoped()

        expect(Number(screen.getByTestId('padding').textContent)).toBeLessThan(comfortable)
        expect(screen.getByTestId('page-bg').textContent).toBe(LIGHT_PALETTE.pageBg)
    })
})
