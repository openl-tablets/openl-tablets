import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    facts: css`
        display: grid;
        grid-template-columns: max-content 1fr;
        gap: ${token.marginXXS}px ${token.marginSM}px;
        margin: 0 0 ${token.margin}px;

        dt {
            color: ${token.colorTextSecondary};
        }

        dd {
            margin: 0;
        }
    `,
    side: css`
        margin-top: ${token.marginXS}px;

        summary {
            cursor: pointer;
            font-weight: ${token.fontWeightStrong};
        }
    `,
    list: css`
        max-height: 50vh;
        margin: ${token.marginXS}px 0 0;
        padding: 0;
        overflow-y: auto;
        list-style: none;

        li {
            display: flex;
            justify-content: space-between;
            gap: ${token.marginSM}px;
            overflow-wrap: anywhere;
        }
    `,
    license: css`
        flex: none;
        padding: 0;
        border: 0;
        background: none;
        color: ${token.colorLink};
        font: inherit;
        cursor: pointer;

        &:hover {
            color: ${token.colorLinkHover};
        }
    `,
}))
