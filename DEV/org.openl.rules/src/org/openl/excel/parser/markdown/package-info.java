/**
 * Reads Markdown files of rules into the same sheet grid an Excel workbook gives, and writes the grid back.
 *
 * <p>A file holds one rules table: its header is the front matter of the file, and its body the GFM table or the
 * code block after it, if any. Everything else in the file is documentation and does not compile. A module is a folder
 * of such files.
 *
 * <p>The table editor writes a table through {@link org.openl.excel.parser.markdown.MarkdownGrid}, which a save
 * writes back into the file. Only the front matter and the body change, and only the keys and rows that changed.
 */
@NullMarked
package org.openl.excel.parser.markdown;

import org.jspecify.annotations.NullMarked;
