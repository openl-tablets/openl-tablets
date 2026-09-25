import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /**
     * The table is as wide as its own text needs and no wider: stretching it to the screen spreads a few short
     * columns across a whole monitor and leaves the reader's eye travelling between them.
     *
     * It is not squeezed into the screen either. A table of hundreds of columns pressed into the width of the
     * page gives each column a few characters and wraps every value into a tower of lines — which is neither
     * what the workbook shows nor something a browser can paint quickly. The screen it sits on scrolls instead.
     */
    table: css`
        border-collapse: collapse;
        /* The table takes the focus so the keyboard reaches it; the picked cell is what shows where it is. */
        outline: none;
        width: max-content;
        max-width: none;
        table-layout: auto;
        font-size: ${token.fontSizeSM}px;
        /*
         * The paper the table is written on. A cell the workbook gave no fill to is white in Excel, not the
         * colour of whatever the table is laid on — and the cells that do carry a fill paint over this.
         */
        background: ${token.colorBgContainer};
    `,
    /**
     * A cell keeps the line breaks the author wrote, and wraps a long value instead of widening its column past
     * the screen — the way the workbook itself shows it.
     */
    cell: css`
        border: 1px solid ${token.colorBorderSecondary};
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        text-align: left;
        vertical-align: top;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        max-width: 420px;
    `,
    /**
     * The number of a line of data, beside the table rather than in it: the workbook has no such cell, and a
     * reader counting cases down a long table should not have to count rows to find the third one.
     */
    lineNumber: css`
        border: none;
        /* The ground the table is laid on, so the numbers read as the margin they are and not as cells. */
        background: ${token.colorBgLayout};
        padding: 0 ${token.paddingXS}px;
        color: ${token.colorText};
        font-size: ${token.fontSizeSM}px;
        text-align: right;
        vertical-align: middle;
        white-space: nowrap;
        user-select: none;
    `,
    /**
     * A cell a reader left a note on in Excel, marked in its corner the way the old editor marked it.
     *
     * <p>The mark is drawn by the cell itself rather than by an image, so it costs the page nothing.
     */
    commented: css`
        position: relative;

        &::after {
            content: '';
            position: absolute;
            top: 0;
            right: 0;
            border-top: 6px solid ${token.colorError};
            border-left: 6px solid transparent;
        }
    `,
    /** The note itself, kept to the width the old editor gave it and to the lines its author typed. */
    note: css`
        display: block;
        max-width: 160px;
        white-space: pre-line;
    `,
    /**
     * A piece of a cell's text the compiler resolved.
     *
     * A formula is read as a sentence, and a rule under every word of it is a sentence that cannot be read —
     * so a piece that leads nowhere is left as it stands and says what it is in its tooltip alone.
     */
    /**
     * A piece that names a table reads as the way into it that it is, and underlines under the pointer.
     *
     * <p>It is a button, so it is reached by the keyboard as every other way into a table is; the button's
     * own frame is dropped, since what is drawn is a word inside a sentence.
     */
    usageLink: css`
        padding: 0;
        border: none;
        background: none;
        font: inherit;
        cursor: pointer;
        color: ${token.colorLink};

        &:hover,
        &:focus-visible {
            color: ${token.colorLinkHover};
            text-decoration: underline;
        }
    `,
}))

/**
 * The table's styles, as the table that draws it reads them.
 *
 * <p>Read once per table and handed to the cells. `useStyles()` copies the whole theme for every component
 * that calls it, so a hook in a cell is a copy of the theme per cell — a table of a few hundred marked cells
 * then costs tens of megabytes and the tab stops answering while it is drawn.
 */
export type RawTableGridStyles = ReturnType<typeof useStyles>['styles']
