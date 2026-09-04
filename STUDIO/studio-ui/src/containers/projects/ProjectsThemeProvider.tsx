import type { PropsWithChildren } from 'react'
import { ConfigProvider } from 'antd'
import { PROJECTS_THEME } from './projectsTheme'

/**
 * Scopes the mockup-matching light theme to the Projects tab. Wrapping only the two `/projects` route
 * elements keeps the token overrides (indigo primary, tighter radii, muted greys) confined to this
 * subtree — the shared Header, Editor and Administration screens keep the default Studio theme. Because
 * antd-style reads its `createStyles` token from the nearest `ConfigProvider`, co-located styles resolve
 * to these tokens too.
 */
export const ProjectsThemeProvider = ({ children }: PropsWithChildren) => (
    <ConfigProvider theme={PROJECTS_THEME}>{children}</ConfigProvider>
)
