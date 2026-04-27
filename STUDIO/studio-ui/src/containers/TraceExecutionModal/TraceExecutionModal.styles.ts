import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    modal: css`
        .ant-result {
            padding: 24px 16px;
        }

        .ant-result-icon {
            margin-bottom: 16px;
        }

        .ant-result-title {
            font-size: 18px;
            font-weight: 500;
        }

        .ant-result-subtitle {
            margin-top: 8px;
            color: rgb(0 0 0 / 45%);
        }
    `,
}))
