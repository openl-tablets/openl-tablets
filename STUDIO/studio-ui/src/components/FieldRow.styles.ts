import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    row: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
    `,
    rowTop: css`
        align-items: flex-start;
    `,
    label: css`
        flex: none;
        width: 110px;
        text-align: right;
        font-size: 14px;
        color: ${token.colorText};

        /* The colon belongs to the label's presentation, as it does in the administration forms. */
        &::after {
            content: ':';
        }
    `,
    labelTop: css`
        padding-top: 6px;
    `,
    required: css`
        /* The asterisk is presentation, like the colon: it marks the label, it is not part of its text. */
        &::before {
            content: '*';
            margin-right: 3px;
            color: ${token.colorError};
        }
    `,
    control: css`
        flex: 1;
        min-width: 0;
    `,
}))
