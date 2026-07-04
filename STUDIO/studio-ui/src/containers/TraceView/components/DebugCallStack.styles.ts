import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        height: 100%;
        overflow: auto;
        display: flex;
        flex-direction: column;
    `,
    header: css`
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextTertiary};
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        background: ${token.colorFillQuaternary};
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    frame: css`
        display: flex;
        align-items: center;
        cursor: pointer;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        border-left: 2px solid transparent;
        transition: background ${token.motionDurationMid};
        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    frameSelected: css`
        background: ${token.colorPrimaryBg};
    `,
    frameCurrent: css`
        border-left-color: ${token.colorPrimary};
    `,
    name: css`
        font-weight: 500;
    `,
    location: css`
        color: ${token.colorTextSecondary};
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
    `,
}))
