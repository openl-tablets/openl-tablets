package org.openl.excel.parser.markdown;

import org.jspecify.annotations.Nullable;

/**
 * A cell written in a table of a Markdown file, whatever the syntax of the table.
 *
 * @param text    the text the cell holds, or {@code null} for a blank cell
 * @param rowspan the rows the cell spans, more than one for a merged cell
 * @param colspan the columns the cell spans, more than one for a merged cell
 */
record TableCell(@Nullable String text, int rowspan, int colspan) {

    /**
     * Returns a cell of one row and one column.
     */
    static TableCell of(@Nullable String text) {
        return new TableCell(text, 1, 1);
    }

    /**
     * Tells whether the cell spans more than one row or column.
     */
    boolean merged() {
        return rowspan > 1 || colspan > 1;
    }
}
