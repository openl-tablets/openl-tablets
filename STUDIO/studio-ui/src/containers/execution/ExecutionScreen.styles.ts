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
    /** A list drawn as the window scrolls: as tall as everything in it, the part in sight laid at its place. */
    scrolledList: css`
        position: relative;
        width: 100%;
    `,
    /** An entry of a list drawn as the window scrolls, with the room an entry keeps from the next one. */
    scrolledEntry: css`
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        padding-bottom: ${token.margin}px;
    `,
}))
