import { createGlobalStyle } from 'antd-style'
import { paletteVariables } from './styles/listPageTheme'
import './styles/customToken'

/**
 * Publishes the palette of the theme and appearance in force as CSS custom properties, so every style that
 * refers to a colour through `LIST_PAGE_COLORS` repaints when the user switches either, and paints the page
 * itself in the matching surface and text colour.
 */
export const AppStyles = createGlobalStyle(({ theme }) => `
    :root {
        ${paletteVariables(theme.openl)}
        color-scheme: ${theme.isDarkMode ? 'dark' : 'light'};
    }

    html, body {
        color: ${theme.colorText};
        background: ${theme.colorBgContainer};
        margin: 0;
    }
`)
