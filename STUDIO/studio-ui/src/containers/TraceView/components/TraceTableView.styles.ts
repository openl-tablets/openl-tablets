import { createStyles } from 'antd-style'
import { tableOf } from '../../../styles/tableColours'

export const useStyles = createStyles(({ css, token }) => ({
    card: css`
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
    loading: css`
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100px;
    `,
    content: css`
        overflow: auto;
        max-width: 100%;
    `,
    // Everything that is NOT part of the highlighted calculation, muted to grey (like the legacy trace),
    // so the highlighted cells are the only colour on the table and the eye lands on them instantly.
    dimmed: css`
        filter: grayscale(1);
        opacity: 0.6;
    `,
    /* One execution-state colour language, shared with the spreadsheet grid, decision panel and legend.
       Matches the legacy trace: a matched condition and the returned result are green, an unmatched
       condition is red. The result stands apart from a plain matched condition by a bold green border.
       The marks lie on the paper of the table, in the colours it gives them, and the legend keys them with these
       classes. */
    current: css`
        background: ${tableOf(token).marks.colorWarningBg};
    `,
    result: css`
        background: ${tableOf(token).marks.colorSuccessBg};
        box-shadow: inset 0 0 0 1px ${tableOf(token).marks.colorSuccess};
        font-weight: 600;
    `,
    conditionTrue: css`
        background: ${tableOf(token).marks.colorSuccessBg};
    `,
    conditionFalse: css`
        background: ${tableOf(token).marks.colorErrorBg};
    `,
    // The colour key, shown under the table — but only for the states this table actually paints.
    legend: css`
        display: flex;
        flex-wrap: wrap;
        gap: ${token.marginSM}px;
        margin-top: ${token.marginXS}px;
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
    truncated: css`
        margin-top: ${token.marginXS}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
}))
