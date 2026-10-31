package org.openl.excel.parser.markdown;

import java.util.List;

/**
 * Writes the lines of a table of a Markdown file from rows of cells, whatever syntax writes it: a GFM table or an HTML
 * table.
 *
 * <p>The syntax stays inside the builder: it is given the rows and the cells of the table, a merged cell with the rows
 * and the columns it spans, or the line of a row the file keeps as written. A row stands on a line of its own.
 */
sealed interface MarkdownTableBuilder permits GfmTableBuilder, HtmlTableBuilder {

    /**
     * Writes a row of cells.
     *
     * @throws MarkdownWriteException when a value has no text of a cell that reads back as the value
     */
    MarkdownTableBuilder row(List<TableCell> cells);

    /**
     * Keeps a row as the file writes it.
     *
     * @param line the line the row stands on
     */
    MarkdownTableBuilder written(String line);

    /**
     * Returns the lines of the table.
     */
    List<String> build();

    /**
     * Returns a value with its line breaks written as {@code \n}.
     */
    static String normalized(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
