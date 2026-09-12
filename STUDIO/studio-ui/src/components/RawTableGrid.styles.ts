import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /**
     * The table is as wide as its own text needs and no wider: stretching it to the screen spreads a few short
     * columns across a whole monitor and leaves the reader's eye travelling between them.
     *
     * It is not squeezed into the screen either. A table of hundreds of columns pressed into the width of the
     * page gives each column a few characters and wraps every value into a tower of lines — which is neither
     * what the workbook shows nor something a browser can paint quickly. The screen it sits on scrolls instead.
     */
    table: css`
        border-collapse: collapse;
        width: max-content;
        max-width: none;
        table-layout: auto;
        font-size: ${token.fontSizeSM}px;
    `,
    /**
     * A cell keeps the line breaks the author wrote, and wraps a long value instead of widening its column past
     * the screen — the way the workbook itself shows it.
     */
    cell: css`
        border: 1px solid ${token.colorBorderSecondary};
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        text-align: left;
        vertical-align: top;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        max-width: 420px;
    `,
}))
