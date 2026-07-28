import { createStyles } from 'antd-style'

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
    table: css`
        border-collapse: collapse;
        width: 100%;
        font-size: ${token.fontSizeSM}px;
    `,
    cell: css`
        border: 1px solid ${token.colorBorderSecondary};
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        text-align: left;
        vertical-align: top;
        white-space: pre-wrap;
    `,
    // Everything that is NOT part of the highlighted calculation, muted to grey (like the legacy trace),
    // so the highlighted cells are the only colour on the table and the eye lands on them instantly.
    dimmed: css`
        filter: grayscale(1);
        opacity: 0.6;
    `,
    /* One execution-state colour language, shared with the spreadsheet grid, decision panel and legend.
       Matches the legacy trace: a matched condition and the returned result are green, an unmatched
       condition is red. The result stands apart from a plain matched condition by a bold green border. */
    current: css`
        background: ${token.colorWarningBg};
    `,
    result: css`
        background: ${token.colorSuccessBg};
        box-shadow: inset 0 0 0 1px ${token.colorSuccess};
        font-weight: 600;
    `,
    conditionTrue: css`
        background: ${token.colorSuccessBg};
    `,
    conditionFalse: css`
        background: ${token.colorErrorBg};
    `,
    truncated: css`
        margin-top: ${token.marginXS}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
}))
