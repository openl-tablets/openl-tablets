import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /**
     * The table is as wide as its own text needs and no wider: stretching it to the screen spreads a few short
     * columns across a whole monitor and leaves the reader's eye travelling between them.
     */
    table: css`
        border-collapse: collapse;
        width: auto;
        max-width: 100%;
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
