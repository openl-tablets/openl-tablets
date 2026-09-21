import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    deletedFileStatus: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        height: 24px;
        padding-inline: 7px;
    `,
    /** The heading of the scrolling conflict list, which stays put while the list moves under it. */
    conflictsHeading: css`
        display: block;
        position: sticky;
        top: 0;
        z-index: 1;
        margin-bottom: ${token.marginXS}px;
        padding-bottom: ${token.paddingXS}px;
        background: ${token.colorBgElevated};
    `,
    actions: css`
        display: flex;
        justify-content: flex-end;
        padding-top: ${token.paddingXS}px;
        border-top: 1px solid ${token.colorSplit};
    `,
}))
