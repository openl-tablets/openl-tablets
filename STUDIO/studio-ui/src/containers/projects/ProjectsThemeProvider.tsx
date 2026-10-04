import { useMemo, type PropsWithChildren } from 'react'
import { ThemeProvider, useTheme, useThemeMode } from 'antd-style'
import { projectsTheme } from './projectsTheme'
import { densityTheme, useAppTheme } from '../../providers/AppThemeProvider'

/**
 * Scopes the mockup-matching theme to the Projects tab. Wrapping only the two `/projects` route elements
 * keeps the token overrides (tighter radii, taller controls, muted tabs) confined to this subtree — the
 * shared Header, Editor and Administration screens keep the shape of the application theme.
 *
 * It is an antd-style `ThemeProvider` rather than a bare Ant Design `ConfigProvider` because `createStyles`
 * reads its token from the nearest **antd-style** provider; a plain `ConfigProvider` would restyle the Ant
 * Design components but leave the co-located styles on the application-wide token.
 *
 * The appearance the user picked is passed straight through, so the Projects screens turn light or dark
 * with the rest of the application and keep following the system under `auto`. The colours are those of the
 * picked theme: Ant Design hands the application's tokens down to a nested theme, and the mockup's tabs and
 * buttons take the picked theme's palette. The density is merged in for a different reason: a theme of
 * its own replaces the algorithm chain, so without it the Projects screens would stay comfortable inside a
 * compact application.
 */
export const ProjectsThemeProvider = ({ children }: PropsWithChildren) => {
    const { themeMode } = useThemeMode()
    const { compact } = useAppTheme()
    // The palette of the theme and appearance in force, as the application's provider carries it.
    const { openl } = useTheme()
    const theme = useMemo(() => ({ ...projectsTheme(openl), ...densityTheme(compact) }), [compact, openl])

    return <ThemeProvider theme={theme} themeMode={themeMode}>{children}</ThemeProvider>
}
