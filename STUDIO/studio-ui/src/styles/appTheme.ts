import type { ThemeConfig } from 'antd'
import type { Palette } from './listPageTheme'

/**
 * The Ant Design theme a palette describes, for the whole application.
 *
 * Only the semantic hues are handed over — the primary, the link and the four states. The surfaces, the
 * borders and the text stay with the appearance algorithm, which already derives a consistent set of them
 * for light and for dark; overriding those as well would make each theme responsible for a whole greyscale
 * and gain nothing, since the palette's own surfaces are published as custom properties anyway.
 */
export const appTheme = (palette: Palette): ThemeConfig => ({
    token: {
        colorPrimary: palette.primary,
        colorLink: palette.primary,
        colorInfo: palette.info,
        colorSuccess: palette.success,
        colorWarning: palette.warning,
        colorError: palette.error,
    },
})
