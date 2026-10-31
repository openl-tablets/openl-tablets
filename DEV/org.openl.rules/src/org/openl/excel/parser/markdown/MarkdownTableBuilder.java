package org.openl.excel.parser.markdown;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Writes the lines of a table of a Markdown file from rows of cells.
 *
 * <p>The syntax stays inside the builder: it is given the values of the cells of a row, or the line of a row the file
 * keeps as written. A row stands on a line of its own.
 */
sealed interface MarkdownTableBuilder permits GfmTableBuilder {

    /**
     * Writes a row of cells.
     *
     * @param cells the values of the cells; a blank cell is {@code null}
     * @throws MarkdownWriteException when a value has no text of a cell that reads back as the value
     */
    MarkdownTableBuilder row(List<@Nullable String> cells);

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
