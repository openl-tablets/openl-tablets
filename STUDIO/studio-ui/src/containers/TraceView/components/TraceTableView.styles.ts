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

        table {
            border-collapse: collapse;
            width: 100%;
        }

        td,
        th {
            border: 1px solid ${token.colorBorderSecondary};
            padding: ${token.paddingXXS}px ${token.paddingXS}px;
            font-size: ${token.fontSizeSM}px;
        }

        th {
            background: ${token.colorFillQuaternary};
            font-weight: 500;
        }

        /* One execution-state colour language, shared with the spreadsheet grid and decision panel:
           amber = the current line, green = a result, blue = a passed condition, red = a failed one. */
        .trace-highlighted {
            background: ${token.colorWarningBg};
        }

        .trace-result {
            background: ${token.colorSuccessBg};
            font-weight: 600;
        }

        .trace-condition-true {
            background: ${token.colorInfoBg};
        }

        .trace-condition-false {
            background: ${token.colorErrorBg};
        }
    `,
}))
