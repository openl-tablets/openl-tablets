import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    table: css`
        border-collapse: collapse;
        width: 100%;
        font-size: ${token.fontSizeSM}px;
    `,
    cell: css`
        border: 1px solid ${token.colorBorderSecondary};
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        text-align: left;
        vertical-align: top;
        white-space: pre-wrap;
    `,
}))
