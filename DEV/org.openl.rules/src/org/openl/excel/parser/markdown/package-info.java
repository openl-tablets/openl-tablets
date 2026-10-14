/**
 * Reads Markdown files of rules into the same sheet grid an Excel workbook gives.
 *
 * <p>A file holds one rules table: its header is the front matter of the file, and its body the GFM table or the
 * code block after it. Everything else in the file is documentation and does not compile. A module is a folder of
 * such files.
 */
@NullMarked
package org.openl.excel.parser.markdown;

import org.jspecify.annotations.NullMarked;
