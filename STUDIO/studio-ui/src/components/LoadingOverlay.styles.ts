import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    // Transparent click shield: blocks interaction without dimming the page,
    // above every legacy popup (they use z-indexes below 10000).
    overlay: css`
        position: fixed;
        inset: 0;
        z-index: 10000;
        display: flex;
        align-items: center;
        justify-content: center;
    `,
}))
