import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from 'styles/listPageTheme'

/** What a list table adds to the shared list look: it fills its container and may scroll inside. */
export const useStyles = createStyles(({ css }) => ({
    scroll: css`
        overflow: auto;
    `,
    table: css`
        width: 100%;
        margin: 0;
        table-layout: auto;
    `,
    /** The header stays in view while the rows below it scroll. */
    stickyHead: css`
        th {
            position: sticky;
            top: 0;
            z-index: 1;
            background: ${LIST_PAGE_COLORS.containerBg};
        }
    `,
    fit: css`
        width: 1px;
        white-space: nowrap;
    `,
    center: css`
        text-align: center !important;
    `,
    right: css`
        text-align: right !important;
    `,
    clickable: css`
        cursor: pointer;
    `,
    /** A list that is only read gets no pointer tint: nothing happens when a row is clicked. */
    noHover: css`
        &:hover td {
            background: ${LIST_PAGE_COLORS.containerBg};
        }
    `,
    selectedRow: css`
        td {
            background: ${LIST_PAGE_COLORS.accent};
        }

        &:hover td {
            background: ${LIST_PAGE_COLORS.accent};
        }
    `,
}))
