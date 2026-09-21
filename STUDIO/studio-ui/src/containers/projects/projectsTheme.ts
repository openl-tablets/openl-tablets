import type { ThemeConfig } from 'antd'
import { LIST_PAGE_COLORS, Palette } from '../../styles/listPageTheme'

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
 * It takes the palette of the theme and appearance in force, so the Projects screens follow both choices.
 * The shape — radii, control height, type — is stated as **seed** tokens, which a density algorithm scales;
 * the same measurement written as a per-component override would stand still while the rest of the screen
 * tightened.
 */
export const projectsTheme = (palette: Palette): ThemeConfig => {
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
            // The mockup stands its controls 4px above the Ant Design default, in either density.
            controlHeight: 36,
            wireframe: false,
        },
        components: {
            Tabs: {
                // Tighter than Ant Design's 32, so the tab strip of a project reads as one group. The
                // vertical padding is left to the default `paddingSM`, which the density algorithm scales.
                horizontalItemGutter: 20,
                inkBarColor: palette.primary,
                itemColor: palette.textTertiary,
                itemSelectedColor: palette.text,
                itemHoverColor: palette.text,
            },
            Button: {
                defaultBorderColor: palette.border,
                primaryShadow: 'none',
                defaultShadow: 'none',
            },
            Segmented: {
                trackBg: palette.secondaryBg,
            },
        },
    }
}
