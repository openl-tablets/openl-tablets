import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** The palette, laid out as Excel lays it out: the theme colours, their shades, then the standard colours. */
    palette: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXXS}px;
    `,
    /** The name of a group of colours, as Excel names it above the swatches. */
    heading: css`
        color: ${token.colorTextSecondary};
        font-size: ${token.fontSizeSM}px;
    `,
    /** The shades, set apart from the theme colours above them and touching one another, as Excel draws them. */
    shades: css`
        display: flex;
        flex-direction: column;
    `,
    row: css`
        display: flex;
        gap: ${token.marginXXS}px;
    `,
    swatch: css`
        width: 16px;
        height: 16px;
        padding: 0;
        border: 1px solid ${token.colorBorderSecondary};
        cursor: pointer;

        &:hover {
            outline: 1px solid ${token.colorPrimary};
            position: relative;
        }
    `,
}))
