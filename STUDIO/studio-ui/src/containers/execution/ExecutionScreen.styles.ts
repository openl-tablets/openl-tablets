import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    window: css`
        padding: ${token.paddingLG}px;
    `,
    actions: css`
        margin-top: ${token.marginLG}px;
    `,
    empty: css`
        margin-top: ${token.marginXL * 2}px;
    `,
}))
