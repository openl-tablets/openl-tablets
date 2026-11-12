import { createGlobalStyle } from 'antd-style'
import { paletteVariables } from './styles/listPageTheme'
import './styles/customToken'

/**
 * Publishes the palette of the theme and appearance in force as CSS custom properties, so every style that
 * refers to a colour through `LIST_PAGE_COLORS` repaints when the user switches either, and paints the page
 * itself in the matching surface and text colour.
 *
 * A node of a tree that cannot be picked — a group that is only opened, a value of a run, a field of the input
 * form — reads as the nodes beside it. Ant Design draws such a node as a disabled one, greyed and under a forbidden
 * cursor; a node that is disabled indeed keeps that look.
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

    .ant-tree .ant-tree-treenode-unselectable:not(.ant-tree-treenode-disabled) .ant-tree-node-content-wrapper {
        color: inherit;
        cursor: default;
    }
`)
