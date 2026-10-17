package org.openl.excel.parser.markdown;

import org.apache.poi.ss.util.CellAddress;
import org.jspecify.annotations.Nullable;

/**
 * A place in a Markdown file that breaks the format.
 *
 * <p>A problem in the body of a table leaves the table readable: the table is read without the part that breaks
 * the format, such as a merged range over a value. A problem in the front matter leaves the file without a table.
 *
 * @param line    the line of the file, counted from one
 * @param problem what is wrong
 * @param cell    the cell of the sheet the problem shows in, or {@code null} when no cell of the table shows it
 */
public record MarkdownProblem(int line, String problem, @Nullable CellAddress cell) {
}
