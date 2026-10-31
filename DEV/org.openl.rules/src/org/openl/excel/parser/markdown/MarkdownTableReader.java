package org.openl.excel.parser.markdown;

import java.util.List;

/**
 * Reads a table written in a Markdown file as rows of cells, whatever syntax writes it: a GFM table or an HTML table.
 *
 * <p>The syntax stays inside the reader: the rows and the cells it gives are those of the table, a merged cell with
 * the rows and the columns it spans. {@link TableLayout} lays them out in the body of the table.
 */
sealed interface MarkdownTableReader permits GfmTableReader, HtmlTableReader {

    /**
     * Returns the rows of the table, in their order, each with the cells written in it.
     */
    List<TableRow> getRows();
}
