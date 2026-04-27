import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    card: css`
        .ant-card-head {
            min-height: 36px;
            padding: 0 12px;
        }

        .ant-card-head .ant-card-head-title {
            padding: 8px 0;
            font-size: 13px;
        }

        .ant-card-body {
            padding: 12px;
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
            border: 1px solid #d9d9d9;
            padding: 4px 8px;
            font-size: 12px;
        }

        th {
            background: #fafafa;
            font-weight: 500;
        }

        .trace-highlighted {
            background: #fffbe6;
        }

        .trace-result {
            background: #f6ffed;
            font-weight: 600;
        }

        .trace-condition-true {
            background: #e6f7ff;
        }

        .trace-condition-false {
            background: #fff1f0;
        }
    `,
}))
