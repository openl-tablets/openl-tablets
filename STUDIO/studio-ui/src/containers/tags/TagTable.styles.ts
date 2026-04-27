import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    table: css`
        tbody .editable-cell-wrap {
            padding: 5px 12px;
            cursor: pointer;
        }

        tbody tr:hover td .editable-cell-wrap {
            padding: 4px 24px 4px 11px;
            border: 1px solid #d9d9d9;
            border-radius: 2px;
        }
    `,
}))
