/**
 * The table themes: the looks OpenL Studio gives the kinds of table
 * {@link org.openl.studio.projects.service.tables.theme.TableRegionLayout#styles} names.
 *
 * <p>Each theme is a {@code table-themes/*.yaml} file of the Studio classpath, known by the name of its file. A
 * theme is one style for every kind of table it styles. It is used in two ways. It can be written into the workbook,
 * or drawn over a table on the screen while the workbook stays as it is.
 *
 * <p>Both ways draw the theme from the regions {@link org.openl.studio.projects.service.tables.theme.TableRegionLayout}
 * lays a table out in, so the screen shows what writing the theme would give. A table is laid out once for each
 * compilation of it, whatever theme it is drawn in.
 */
@NullMarked
package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.NullMarked;
