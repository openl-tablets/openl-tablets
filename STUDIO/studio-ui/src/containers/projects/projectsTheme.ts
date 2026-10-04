import type { ThemeConfig } from 'antd'
import { LIST_PAGE_COLORS, type Palette } from '../../styles/listPageTheme'

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
 * The compilation state — OpenL's core signal — keyed by {@code ProjectCompileState}, in the hues of the four
 * states.
 *
 * Each hue is a CSS custom property of the palette in force, so a state keeps its meaning and still repaints
 * when the theme or the appearance changes. A state that was never compiled and one whose compilation was
 * cancelled read the same, so they share one muted hue.
 */
export const COMPILE_COLORS = {
    ok: LIST_PAGE_COLORS.success,
    warnings: LIST_PAGE_COLORS.warning,
    errors: LIST_PAGE_COLORS.error,
    compiling: LIST_PAGE_COLORS.info,
    cancelled: LIST_PAGE_COLORS.textTertiary,
    idle: LIST_PAGE_COLORS.textTertiary,
} as const

/**
 * Ant Design theme scoped to the Projects tab only (mounted by {@link ProjectsThemeProvider} around the two
 * `/projects` route elements). It never leaks to the shared Header, Editor or Administration screens.
 *
 * It adds the shape of the mockup to the theme of the application, whose colours it inherits, and draws the
 * tabs and the segmented controls of the mockup in the palette of the theme and appearance in force.
 * The shape — radii, control height, type — is stated as **seed** tokens, which a density algorithm scales;
 * the same measurement written as a per-component override would stand still while the rest of the screen
 * tightened.
 */
export const projectsTheme = (palette: Palette): ThemeConfig => {
    return {
        token: {
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
                itemColor: palette.textTertiary,
                itemSelectedColor: palette.text,
                itemHoverColor: palette.text,
            },
            Button: {
                primaryShadow: 'none',
                defaultShadow: 'none',
            },
            Segmented: {
                trackBg: palette.secondaryBg,
            },
        },
    }
}
