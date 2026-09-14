import { createStyles } from 'antd-style'

/** The frame around one group of values in the trace window: the title and the panel it sits in. */
export const useStyles = createStyles(({ css, token }) => ({
    section: css`
        background: ${token.colorFillQuaternary};
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        margin-bottom: ${token.marginSM}px;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        line-height: 1.6;
    `,
    header: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        margin-bottom: ${token.marginXXS}px;
    `,
    title: css`
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextTertiary};
    `,
    empty: css`
        font-style: italic;
        color: ${token.colorTextTertiary};
    `,
}))
