package org.openl.excel.parser.markdown;

import java.util.List;

/**
 * A row written in a table of a Markdown file, whatever the syntax of the table.
 *
 * @param lines the lines of the file the row stands on
 * @param cells the cells written in the row, in their order; a cell a merged cell covers is not written
 */
record TableRow(TableFile.Block lines, List<TableCell> cells) {
}
