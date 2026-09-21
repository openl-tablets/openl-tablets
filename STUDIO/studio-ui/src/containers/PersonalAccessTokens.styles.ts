import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    rowExpired: css`
        background-color: ${token.colorErrorBg};

        &:hover > td {
            background-color: ${token.colorErrorBgHover} !important;
        }
    `,
    codeBlock: css`
        position: relative;
        background-color: ${token.colorFillQuaternary};
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
        padding: ${token.padding}px;
        padding-right: 48px;

        pre {
            margin: 0;
            overflow-x: auto;
            white-space: pre-wrap;
            overflow-wrap: break-word;
        }

        pre code {
            font-family: ${token.fontFamilyCode};
            font-size: ${token.fontSizeSM}px;
            line-height: 1.5;
            color: ${token.colorText};
        }
    `,
    codeBlockNoCopy: css`
        padding-right: ${token.padding}px;
    `,
    codeBlockCopy: css`
        position: absolute;
        top: ${token.paddingXS}px;
        right: ${token.paddingXS}px;
        color: ${token.colorTextTertiary};
        border: 1px solid transparent;
        border-radius: ${token.borderRadius}px;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        height: auto;

        &:hover {
            background-color: ${token.colorFillSecondary};
            border-color: ${token.colorBorderSecondary};
            color: ${token.colorText};
        }
    `,
}))
