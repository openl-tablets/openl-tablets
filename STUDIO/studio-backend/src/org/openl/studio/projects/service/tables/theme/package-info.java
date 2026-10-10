/**
 * The table theme: the look OpenL Studio gives the kinds of table
 * {@link org.openl.studio.projects.service.tables.theme.ThemeLayouts#styles} names.
 *
 * <p>The theme is the {@code table-theme.yaml} file of the Studio classpath: one style for every kind of table it
 * styles, its colours theme colours of Excel. It is used in two ways. It can be written into the workbook, or drawn
 * over a table on the screen while the workbook stays as it is.
 *
 * <p>Both ways ask {@link org.openl.studio.projects.service.tables.theme.ThemeLayouts} which cell gets which look, so
 * the screen shows what writing the theme would give.
 */
@NullMarked
package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.NullMarked;
