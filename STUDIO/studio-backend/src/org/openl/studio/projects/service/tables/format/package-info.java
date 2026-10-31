/**
 * The OpenL table style: the roles of the cells of a table, and the formatter that writes them into a workbook.
 *
 * <p>{@link org.openl.studio.projects.service.tables.format.TableRegionLayout} lays a table out in regions, the
 * rectangles of its cells that take one role. The screen draws a table from its regions, and
 * {@link org.openl.studio.projects.service.tables.format.ExcelTableFormatter} writes the style of each role into the
 * workbook from them, so the screen and the file show a table alike.
 *
 * <p>A table is laid out once for each compilation of it. Only {@code .xlsx} workbooks are formatted. A table whose
 * file keeps no formatting, a table of a Markdown module, is read in the style through
 * {@link org.openl.studio.projects.service.tables.format.StyledGrid}, as if it were formatted.
 */
@NullMarked
package org.openl.studio.projects.service.tables.format;

import org.jspecify.annotations.NullMarked;
