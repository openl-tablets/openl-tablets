/**
 * The table themes: the looks OpenL Studio gives Datatype and Vocabulary tables.
 *
 * <p>Each theme is a {@code table-themes/*.yaml} file of the Studio classpath, known by the name of its file. A
 * theme styles only the kinds of table it names a look for. It is used in two ways. It can be written into the
 * workbook, or drawn over a table on the screen while the workbook stays as it is.
 *
 * <p>Both ways ask {@link org.openl.studio.projects.service.tables.theme.DatatypeThemeLayout} which cell gets
 * which look, so the screen shows what writing the theme would give.
 */
@NullMarked
package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.NullMarked;
