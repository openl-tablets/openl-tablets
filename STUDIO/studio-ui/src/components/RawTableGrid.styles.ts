import { createStyles } from 'antd-style'
import type { TablePaper } from '../styles/paper'

/**
 * How wide a cell is at the least, in pixels: the width Excel gives a column its author never set.
 *
 * <p>A column nothing is written in keeps room to write in rather than closing to a line. A column with
 * something in it is drawn at the width that something needs, which is more than this.
 */
const MIN_CELL_WIDTH = 64

/**
 * The styles of a table of a workbook. The cells are drawn on the paper the table is laid on ({@link TablePaper}); the
 * margin beside the table — the numbers of its lines — belongs to the screen and follows the theme.
 */
export const useStyles = createStyles(({ css, token }, paper: TablePaper) => ({
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
        /* The paper and the ink, which a cell's own fill and font colour paint over. */
        background: ${paper.background};
        color: ${paper.text};
    `,
    /**
     * Every row stands at least one line tall, as every row of the workbook does.
     *
     * <p>A row whose cells all run on into the rows below it has no line of its own, and the browser folds it
     * away. A value merged over that row would then read as covering fewer rules than it does.
     *
     * <p>The height is a little under the line a cell draws, which also holds the part of the text below its
     * baseline: a row with something written in it keeps the height it has.
     */
    row: css`
        height: calc(${Math.round(token.fontSizeSM * token.lineHeightSM) + 2 * token.paddingXXS + 1}px + 0.4em);
    `,
    /**
     * A cell keeps the line breaks the author wrote, and wraps a long value instead of widening its column past
     * the screen — the way the workbook itself shows it.
     *
     * <p>A cell with nothing written in it keeps the room of one that has: a workbook gives every row and
     * column a size of its own whether anything stands there or not, and a row of empty cells drawn to its
     * padding alone reads as a crack between the rules rather than as a line to write in.
     *
     * <p>A cell draws the line of the grid on its right and below it, and above it and on its left only along
     * the edge of the table it lies on, as its `data-edge` names it. Where two cells meet, the browser draws one
     * line. Of two alike, it keeps the line of the cell it lays out first, which is not always the upper or the
     * left one: a cell merged down from a row above comes before the cells on its left. A dotted or a dashed line
     * loses to a solid one. Either way a line of the workbook would lose to the line of the grid. With the grid
     * line drawn by one of the two cells only, the line two cells share is the one the upper or the left cell
     * has, which is where the reader of the workbook hands it.
     */
    cell: css`
        border-right: 1px solid ${paper.grid};
        border-bottom: 1px solid ${paper.grid};
        /* An indented cell leaves the room of its indent beside the padding, on the side it names. */
        padding: ${token.paddingXXS}px calc(${token.paddingXS}px + var(--cell-indent-right, 0em))
            ${token.paddingXXS}px calc(${token.paddingXS}px + var(--cell-indent-left, 0em));
        text-align: left;
        vertical-align: top;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        max-width: 420px;
        min-width: ${MIN_CELL_WIDTH}px;

        /* An empty cell has no line of text to give its row a height, so it is given one that takes no room
           of its own — a row of them then stands as tall as a row with a word in it. Drawn before the text
           rather than after it: the mark of a cell a reader left a note on is the cell's ::after. */
        &::before {
            content: '';
            display: inline-block;
            width: 0;
            height: ${Math.round(token.fontSizeSM * token.lineHeightSM)}px;
        }

        &[data-edge~='top'] {
            border-top: 1px solid ${paper.grid};
        }

        &[data-edge~='left'] {
            border-left: 1px solid ${paper.grid};
        }
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
            border-top: 6px solid ${paper.note};
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
        color: ${paper.link};

        &:hover,
        &:focus-visible {
            color: ${paper.linkHover};
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
