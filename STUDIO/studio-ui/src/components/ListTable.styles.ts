import { createStyles } from 'antd-style'

/**
 * How wide a name of a transposed list asks its column to be, in the size of its own letters. A longer name wraps
 * unless the column has more room to spare.
 */
const LINE_NAME_WIDTH = '24em'

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
            background: ${token.colorBgContainer};
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
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
        text-align: left;
        vertical-align: middle;

        thead tr & {
            z-index: 2;
        }
    `,
    /**
     * The text of a name of a transposed list. It asks its column for no more than a width of its own, so a long name
     * wraps rather than take the room of the values: between its words, before a dot of a field path, or within a
     * word wider than the column.
     *
     * <p>It still takes all the room its column is given, as in a list of a few cases, where the names stay on one
     * line. A percentage would not do: a browser leaves a name with a percentage `min-width` out of the width of its
     * column, which then shrinks to a letter.
     */
    lineNameText: css`
        display: block;
        width: max-content;
        min-width: -moz-available;
        min-width: -webkit-fill-available;
        min-width: stretch;
        max-width: ${LINE_NAME_WIDTH};
        white-space: normal;
        overflow-wrap: anywhere;
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
            background: ${token.colorBgContainer};
        }
    `,
    selectedRow: css`
        td {
            background: ${token.colorPrimaryBg};
        }

        &:hover td {
            background: ${token.colorPrimaryBg};
        }
    `,
}))
