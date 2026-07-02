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
        cursor: pointer;
        gap: ${token.marginXS}px;
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
    addBreakpoint: css`
        width: calc(100% - ${token.paddingSM * 2}px);
        margin: ${token.marginXS}px ${token.paddingSM}px;
    `,
    breakpoint: css`
        gap: ${token.marginXS}px;
    `,
    breakpointDot: css`
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: ${token.colorError};
        flex: 0 0 auto;
    `,
    hint: css`
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
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
