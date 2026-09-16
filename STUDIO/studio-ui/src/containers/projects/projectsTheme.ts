import type { ThemeConfig } from 'antd'
import { DARK_PALETTE, LIGHT_PALETTE, LIST_PAGE_COLORS, Palette } from '../../styles/listPageTheme'

/**
 * Design tokens of the Projects mockup, as CSS custom properties that follow the appearance in force.
 * Used by co-located `createStyles` where a hue must match the mockup and has no Ant Design token
 * (accent hover, sidebar, the code-editor monospace stack).
 */
export const MOCKUP = {
    ...LIST_PAGE_COLORS,
    /** Used only by the file-content `CodeEditor`; every other text keeps the Ant Design font. */
    fontMono: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace",
} as const

/**
 * The compilation state — OpenL's core signal — keyed by {@code ProjectCompileState}.
 *
 * The hues are deliberately distinct from the brand indigo. Each is a CSS custom property of the palette
 * in force, so a state keeps its meaning and still repaints when the appearance changes. A state that was
 * never compiled and one whose compilation was cancelled read the same, so they share one hue.
 */
export const COMPILE_COLORS = {
    ok: LIST_PAGE_COLORS.compileOk,
    warnings: LIST_PAGE_COLORS.compileWarnings,
    errors: LIST_PAGE_COLORS.compileErrors,
    compiling: LIST_PAGE_COLORS.compileCompiling,
    cancelled: LIST_PAGE_COLORS.compileIdle,
    idle: LIST_PAGE_COLORS.compileIdle,
} as const

/**
 * Ant Design theme scoped to the Projects tab only (mounted by {@link ProjectsThemeProvider} around the two
 * `/projects` route elements). It never leaks to the shared Header, Editor or Administration screens.
 *
 * The colours come from the palette of the appearance in force, so the Projects screens follow the theme
 * the user picked. The shape — radii, control heights, tab metrics — is the same in both appearances.
 */
export const projectsTheme = (isDarkMode: boolean): ThemeConfig => {
    const palette: Palette = isDarkMode ? DARK_PALETTE : LIGHT_PALETTE
    return {
        token: {
            colorPrimary: palette.primary,
            colorInfo: palette.info,
            colorSuccess: palette.success,
            colorWarning: palette.warning,
            colorError: palette.error,
            colorTextBase: palette.text,
            colorText: palette.text,
            colorTextSecondary: palette.textSecondary,
            colorTextTertiary: palette.textTertiary,
            colorTextQuaternary: palette.textQuaternary,
            colorBgLayout: palette.pageBg,
            colorBgContainer: palette.containerBg,
            colorBorder: palette.border,
            colorBorderSecondary: palette.borderSecondary,
            borderRadius: 6,
            borderRadiusLG: 8,
            borderRadiusSM: 4,
            fontSize: 14,
            wireframe: false,
        },
        components: {
            Tabs: {
                horizontalItemPadding: '12px 4px',
                horizontalItemGutter: 20,
                inkBarColor: palette.primary,
                itemColor: palette.textTertiary,
                itemSelectedColor: palette.text,
                itemHoverColor: palette.text,
            },
            Button: {
                // Match the 36px Input/Select height so buttons align in compact input groups and toolbars.
                controlHeight: 36,
                defaultBorderColor: palette.border,
                primaryShadow: 'none',
                defaultShadow: 'none',
            },
            Segmented: {
                trackBg: palette.secondaryBg,
                controlHeight: 36,
            },
            Input: {
                controlHeight: 36,
            },
            Select: {
                controlHeight: 36,
            },
        },
    }
}
