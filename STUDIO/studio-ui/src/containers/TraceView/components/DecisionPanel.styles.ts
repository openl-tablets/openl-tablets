import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    card: css`
        margin-top: 12px;
    `,
    summary: css`
        font-weight: 600;
        margin-bottom: 8px;
    `,
    rule: css`
        display: flex;
        align-items: baseline;
        gap: 8px;
        padding: 4px 6px;
        border-radius: 4px;
    `,
    firedRule: css`
        background: ${token.colorSuccessBg};
    `,
    ruleName: css`
        min-width: 90px;
        font-weight: 600;
    `,
    conditions: css`
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
    `,
}))
