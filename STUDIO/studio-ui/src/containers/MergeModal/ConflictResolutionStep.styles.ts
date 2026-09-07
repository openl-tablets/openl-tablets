import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    deletedFileStatus: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        height: 24px;
        padding-inline: 7px;
    `,
}))
