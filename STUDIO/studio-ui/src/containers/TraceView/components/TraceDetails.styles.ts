import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    details: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginSM}px;
        height: 100%;
        overflow: auto;
    `,
    detailsCentered: css`
        align-items: center;
        justify-content: center;
    `,
    tableHeader: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;
    `,
    frameTitle: css`
        font-weight: 600;
        font-size: ${token.fontSizeLG}px;
        color: ${token.colorText};
    `,
    errorsCard: css`
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
    errorLocation: css`
        margin-top: ${token.marginXXS}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
        font-family: ${token.fontFamilyCode};
    `,
    legend: css`
        display: flex;
        flex-wrap: wrap;
        gap: ${token.marginSM}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    legendItem: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
    `,
    swatch: css`
        width: 12px;
        height: 12px;
        border-radius: ${token.borderRadiusSM}px;
        border: 1px solid ${token.colorBorderSecondary};
    `,
    swatchCurrent: css`
        background: ${token.colorWarningBg};
    `,
    swatchResult: css`
        background: ${token.colorSuccessBg};
    `,
    swatchMet: css`
        background: ${token.colorInfoBg};
    `,
    swatchNotMet: css`
        background: ${token.colorErrorBg};
    `,
}))
