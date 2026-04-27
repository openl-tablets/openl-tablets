import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    drawer: css`
        .ant-drawer-body {
            padding: 0;
        }

        .ant-drawer-body .ant-menu {
            border-right: 0;
        }
    `,
}))
