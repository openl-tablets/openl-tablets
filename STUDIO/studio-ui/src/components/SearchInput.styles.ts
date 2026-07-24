import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    search: css`
        .ant-input-prefix .anticon {
            color: ${token.colorTextQuaternary};
        }
    `,
}))
