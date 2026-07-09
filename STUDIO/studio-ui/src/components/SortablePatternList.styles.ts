import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    row: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
    `,
    handle: css`
        cursor: grab;
        color: ${token.colorTextTertiary};
        touch-action: none;

        &:active {
            cursor: grabbing;
        }
    `,
    input: css`
        flex: 1;
    `,
    mono: css`
        &.ant-input {
            font-family: ${token.fontFamilyCode};
        }
    `,
    remove: css`
        color: ${token.colorTextTertiary};
    `,
    add: css`
        padding-left: 0;
    `,
}))
