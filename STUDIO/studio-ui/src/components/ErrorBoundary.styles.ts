import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** The panel of technical detail the development build prints under the apology. */
    details: css`
        margin-top: ${token.marginLG}px;
        padding: ${token.padding}px;
        background-color: ${token.colorFillQuaternary};
        border-radius: ${token.borderRadius}px;
        max-width: 600px;
        overflow: auto;
    `,
    stack: css`
        margin-top: ${token.marginXS}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextSecondary};
        white-space: pre-wrap;
    `,
}))
