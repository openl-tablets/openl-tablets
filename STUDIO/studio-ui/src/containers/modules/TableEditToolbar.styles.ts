import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /**
     * One strip of actions, the way the legacy editor drew it: the buttons run together and a rule separates
     * one group from the next, so the eye reads them as groups rather than as a row of separate controls.
     */
    toolbar: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorFillQuaternary};
    `,
    button: css`
        width: 28px;
        height: 28px;
        padding: 0;
    `,
    /** An action that is already on — the alignment a cell has, a font that is already bold. */
    on: css`
        background: ${token.colorPrimaryBg};
        color: ${token.colorPrimary};
    `,
    rule: css`
        width: 1px;
        height: 18px;
        margin: 0 ${token.marginXXS}px;
        background: ${token.colorBorderSecondary};
    `,
    /** What is left of the strip once the actions have had their say. */
    pending: css`
        margin-left: auto;
        padding-right: ${token.paddingXS}px;
        color: ${token.colorTextSecondary};
        font-size: ${token.fontSizeSM}px;
    `,
}))
