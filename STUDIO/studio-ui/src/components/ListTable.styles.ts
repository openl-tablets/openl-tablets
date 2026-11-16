import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from 'styles/listPageTheme'

/** What a list table adds to the shared list look: it fills its container and may scroll inside. */
export const useStyles = createStyles(({ css, token }) => ({
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
    /**
     * The name of a line of a transposed list, in the first column. It stays in view while the rows scroll across,
     * and the corner above the names stays in view both ways.
     */
    lineName: css`
        position: sticky;
        left: 0;
        z-index: 1;
        padding: ${token.paddingSM}px ${token.paddingSM}px ${token.paddingSM}px ${token.padding}px;
        border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        background: ${LIST_PAGE_COLORS.containerBg};
        text-align: left;
        white-space: nowrap;
        vertical-align: middle;

        thead tr & {
            z-index: 2;
        }
    `,
    /**
     * A line of a transposed list: its first value, which follows the name, is padded as every other value, and the
     * last line draws no rule under its name, as it draws none under its values.
     */
    line: css`
        && td:first-of-type {
            padding-left: ${token.paddingSM}px;
        }

        &&:last-of-type th {
            border-bottom: none;
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
