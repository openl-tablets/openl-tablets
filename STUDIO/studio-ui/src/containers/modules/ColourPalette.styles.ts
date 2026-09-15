import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** The grid of colours, laid out as the legacy editor laid it: rows of swatches with nothing between them. */
    palette: css`
        display: flex;
        flex-direction: column;
        gap: 2px;
        padding-bottom: ${token.paddingXS}px;
    `,
    row: css`
        display: flex;
        gap: 2px;
    `,
    swatch: css`
        width: 16px;
        height: 16px;
        padding: 0;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusXS}px;
        cursor: pointer;

        &:hover {
            outline: 1px solid ${token.colorPrimary};
        }
    `,
}))
