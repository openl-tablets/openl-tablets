import { render, screen } from '@testing-library/react'
import { createStyles } from 'antd-style'
import { beforeEach, describe, expect, it } from 'vitest'
import { ProjectsThemeProvider } from './ProjectsThemeProvider'
import { AppThemeProvider } from '../../providers/AppThemeProvider'
import { DARK_PALETTE, LIGHT_PALETTE } from '../../styles/listPageTheme'
import { THEME_MODE_KEY } from '../../utils/themeMode'

// A co-located style, the way every Projects component writes one — it must see the scoped token.
const useProbeStyles = createStyles(({ css, token }) => ({
    probe: css`background: ${token.colorBgLayout};`,
}))

const StyleProbe = () => {
    const { theme } = useProbeStyles()

    return <span data-testid="page-bg">{theme.colorBgLayout}</span>
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
})
