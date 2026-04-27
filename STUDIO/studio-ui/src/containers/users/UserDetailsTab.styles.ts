import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    emailRow: css`
        margin-inline: 0 !important;

        > .ant-col {
            padding-inline: 0 !important;
        }
    `,
    emailInput: css`
        padding-right: 12px !important;
    `,
}))
