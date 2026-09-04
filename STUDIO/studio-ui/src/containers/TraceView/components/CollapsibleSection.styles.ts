import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXS}px ${token.paddingSM}px ${token.paddingXXS}px;
    `,
    // The title reads as a section label and doubles as the collapse control; reset the button chrome so
    // it looks like the plain label it replaces.
    toggle: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        border: none;
        background: none;
        cursor: pointer;
        padding: 0;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextTertiary};
    `,
    caret: css`
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextQuaternary};
    `,
}))
