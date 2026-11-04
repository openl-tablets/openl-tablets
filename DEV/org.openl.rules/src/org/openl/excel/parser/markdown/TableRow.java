package org.openl.excel.parser.markdown;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A row written in a table of a Markdown file.
 *
 * @param lines the lines of the file the row stands on
 * @param cells the values of the cells written in the row, in their order; a blank cell is {@code null}
 */
record TableRow(TableFile.Block lines, List<@Nullable String> cells) {
}
