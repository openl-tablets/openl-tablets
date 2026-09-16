import { createGlobalStyle } from 'antd-style'
import { DARK_PALETTE, LIGHT_PALETTE, paletteVariables } from './styles/listPageTheme'

/**
 * Publishes the palette of the appearance in force as CSS custom properties, so every style that refers to
 * a colour through `LIST_PAGE_COLORS` repaints when the user switches the theme, and paints the page itself
 * in the matching surface and text colour.
 */
export const AppStyles = createGlobalStyle(({ theme }) => `
    :root {
        ${paletteVariables(theme.isDarkMode ? DARK_PALETTE : LIGHT_PALETTE)}
        color-scheme: ${theme.isDarkMode ? 'dark' : 'light'};
    }

    html, body {
        color: ${theme.colorText};
        background: ${theme.colorBgContainer};
        margin: 0;
    }
`)
