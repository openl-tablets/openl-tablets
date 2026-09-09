import { createStyles } from 'antd-style'

/** The notice a screen shows while the work it started is still going on. */
export const useStyles = createStyles(({ css, token }) => ({
    holder: css`
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        min-height: 200px;
    `,
    /** Over the screen the work belongs to, so that nothing under it is used meanwhile. */
    overlay: css`
        position: absolute;
        inset: 0;
        z-index: 10;
        background: ${token.colorBgMask};
    `,
    card: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: ${token.margin}px;
        max-width: 320px;
        padding: ${token.paddingLG}px ${token.paddingXL}px;
        text-align: center;
        background: ${token.colorBgElevated};
        border-radius: ${token.borderRadiusLG}px;
        box-shadow: ${token.boxShadowSecondary};
    `,
    text: css`
        color: ${token.colorText};
        font-size: ${token.fontSize}px;
    `,
}))
