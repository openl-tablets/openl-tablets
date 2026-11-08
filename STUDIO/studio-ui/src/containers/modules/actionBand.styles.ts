import { createStyles } from 'antd-style'

/**
 * The band of actions over what a screen shows, as the band over a table reads: each action its picture with its
 * name under it.
 */
export const useActionBandStyles = createStyles(({ css, token }) => ({
    /** A band over what is shown, carrying what can be done to it. */
    bar: css`
        display: flex;
        flex: none;
        /* What exercises the table goes under the actions only where the band leaves no room beside them. */
        flex-wrap: wrap;
        align-items: flex-start;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXS}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /** Each action reads as the old toolbar read it: the picture, and its name under it. */
    action: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2px;
        height: auto;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        line-height: 1.2;

        .anticon {
            font-size: ${token.fontSizeLG}px;
        }
    `,
    label: css`
        font-size: ${token.fontSizeSM}px;
    `,
}))
