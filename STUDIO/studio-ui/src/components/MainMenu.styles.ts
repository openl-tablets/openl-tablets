import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    root: css`
        border-inline-end: 1px solid ${token.colorSplit};

        .ant-menu {
            border-right: 0;
        }
    `,
}))
