import type { ThemeConfig } from 'antd'
import { mix } from './colorMath'
import { variantOf, type EditorColors, type ThemeName } from './themes'

/**
 * The seed and surface tokens of a theme drawn from a code editor.
 *
 * The editor's background is the background of every container and its text the text of every screen, and its
 * accent is the primary. The surfaces around the containers are mixed from those two colours in the proportions
 * Ant Design keeps between its own: the page a shade darker than the containers, the borders a share of the text
 * away from them, and in the dark appearance a lifted surface for what floats above the page.
 *
 * Ant Design would derive the surfaces from the background itself, but it shifts their lightness with the
 * saturation of another colour model, which turns a cream into a grey and a slate into a vivid blue.
 */
const editorTokens = ({ background, foreground, primary, text = foreground }: EditorColors, isDarkMode: boolean) => {
    const border = mix(background, text, isDarkMode ? 0.2 : 0.15)
    return {
        colorPrimary: primary,
        colorLink: primary,
        colorTextBase: text,
        colorBgBase: background,
        colorBgContainer: background,
        colorBgLayout: isDarkMode ? mix(background, '#000000', 0.3) : mix(background, text, 0.04),
        colorBgElevated: isDarkMode ? mix(background, text, 0.05) : background,
        colorBorder: border,
        colorBorderDisabled: border,
        colorBorderSecondary: mix(background, text, isDarkMode ? 0.12 : 0.06),
        // A tooltip of the dark appearance; in the light one Ant Design draws it in the text colour itself.
        ...(isDarkMode ? { colorBgSpotlight: border } : {}),
    }
}

/**
 * The Ant Design theme of a theme in the given appearance, for the whole application.
 *
 * The standard theme adds nothing to Ant Design. A theme drawn from a code editor hands over the editor's
 * background, text and accent, so the screens around the editor read as one with it. Every other colour — the
 * hover and active shades, the fills, the softer text — is derived from those by the algorithm of the appearance,
 * which the theme provider adds.
 */
export const appTheme = (name: ThemeName, isDarkMode: boolean): ThemeConfig => {
    const { editor } = variantOf(name, isDarkMode)
    return editor ? { token: editorTokens(editor, isDarkMode) } : {}
}
