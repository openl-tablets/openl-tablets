package org.openl.excel.parser.markdown;

import java.util.List;

/**
 * Reads a table written in a Markdown file as rows of cells.
 *
 * <p>The syntax stays inside the reader: the rows and the values it gives are those of the table, unescaped.
 */
sealed interface MarkdownTableReader permits GfmTableReader {

    /**
     * Returns the rows of the table, in their order, each with the cells written in it.
     */
    List<TableRow> getRows();
}
