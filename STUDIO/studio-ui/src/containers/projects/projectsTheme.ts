import type { GlobalToken, ThemeConfig } from 'antd'
import type { Palette } from '../../styles/listPageTheme'

/** The monospace stack of the file-content `CodeEditor`; every other text keeps the Ant Design font. */
export const FONT_MONO = "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace"

/**
 * The compilation state — OpenL's core signal — keyed by {@code ProjectCompileState}, in the hues of the four
 * states of the token in force.
 *
 * A state that was never compiled and one whose compilation was cancelled read the same, so they share one muted
 * hue.
 */
export const compileColorsOf = (token: GlobalToken) => ({
    ok: token.colorSuccess,
    warnings: token.colorWarning,
    errors: token.colorError,
    compiling: token.colorInfo,
    cancelled: token.colorTextTertiary,
    idle: token.colorTextTertiary,
})

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
export const projectsTheme = (token: GlobalToken, palette: Palette): ThemeConfig => {
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
                itemColor: token.colorTextTertiary,
                itemSelectedColor: token.colorText,
                itemHoverColor: token.colorText,
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
