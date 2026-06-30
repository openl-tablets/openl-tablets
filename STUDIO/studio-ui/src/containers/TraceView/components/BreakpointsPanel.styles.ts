import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        flex: 0 0 auto;
        display: flex;
        flex-direction: column;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    header: css`
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextTertiary};
        padding: ${token.paddingXS}px ${token.paddingSM}px ${token.paddingXXS}px;
    `,
    addBreakpoint: css`
        width: calc(100% - ${token.paddingSM * 2}px);
        margin: 0 ${token.paddingSM}px ${token.marginXS}px;
    `,
    list: css`
        max-height: 140px;
        overflow: auto;
    `,
    breakpoint: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        &:hover {
            background: ${token.colorFillTertiary};
        }
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
        padding: 0 ${token.paddingSM}px ${token.paddingXS}px;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
    `,
    name: css`
        flex: 1;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
    `,
}))
