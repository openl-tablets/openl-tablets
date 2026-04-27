import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    root: css`
        border-inline-end: 1px solid rgb(5 5 5 / 6%);

        .ant-menu {
            border-right: 0;
        }
    `,
}))
