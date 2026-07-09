import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    row: css`
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;
    `,
    mono: css`
        font-family: ${token.fontFamilyCode};
    `,
    remove: css`
        color: ${token.colorTextTertiary};
        flex-shrink: 0;
    `,
    add: css`
        padding-left: 0;
    `,
}))
