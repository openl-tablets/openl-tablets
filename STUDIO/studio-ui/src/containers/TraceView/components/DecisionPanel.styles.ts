import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    card: css`
        margin-top: ${token.marginSM}px;

        .ant-card-head {
            min-height: 36px;
            padding: 0 ${token.paddingSM}px;
        }

        .ant-card-head .ant-card-head-title {
            padding: ${token.paddingXS}px 0;
            font-size: ${token.fontSizeSM}px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: ${token.colorTextTertiary};
        }

        .ant-card-body {
            padding: ${token.paddingSM}px;
        }
    `,
    summary: css`
        font-weight: 600;
        margin-bottom: ${token.marginXS}px;
    `,
    rule: css`
        display: flex;
        align-items: baseline;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        border-radius: ${token.borderRadiusSM}px;
    `,
    firedRule: css`
        background: ${token.colorSuccessBg};
    `,
    ruleName: css`
        min-width: 90px;
        font-weight: 600;
    `,
    conditions: css`
        display: flex;
        flex-wrap: wrap;
        gap: ${token.marginXXS}px;
    `,
}))
