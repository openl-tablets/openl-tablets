import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    details: css`
        display: flex;
        flex-direction: column;
        gap: 12px;
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
        gap: 8px;
    `,
    frameTitle: css`
        font-weight: 600;
    `,
    errorsCard: css`
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
    errorLocation: css`
        margin-top: 4px;
        font-size: 12px;
        color: #8c8c8c;
        font-family: monospace;
    `,
}))
