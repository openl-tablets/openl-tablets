import { useCallback, type PropsWithChildren } from 'react'
import { ThemeProvider, useThemeMode, type ThemeAppearance } from 'antd-style'
import { projectsTheme } from './projectsTheme'

/**
 * Scopes the mockup-matching theme to the Projects tab. Wrapping only the two `/projects` route elements
 * keeps the token overrides (indigo primary, tighter radii, muted greys) confined to this subtree — the
 * shared Header, Editor and Administration screens keep the default Studio theme.
 *
 * It is an antd-style `ThemeProvider` rather than a bare Ant Design `ConfigProvider` because `createStyles`
 * reads its token from the nearest **antd-style** provider; a plain `ConfigProvider` would restyle the Ant
 * Design components but leave the co-located styles on the application-wide token.
 *
 * The appearance the user picked is passed straight through, so the Projects screens turn light or dark
 * with the rest of the application and keep following the system under `auto`.
 */
export const ProjectsThemeProvider = ({ children }: PropsWithChildren) => {
    const { themeMode } = useThemeMode()
    const theme = useCallback((appearance: ThemeAppearance) => projectsTheme(appearance === 'dark'), [])

    return <ThemeProvider theme={theme} themeMode={themeMode}>{children}</ThemeProvider>
}
