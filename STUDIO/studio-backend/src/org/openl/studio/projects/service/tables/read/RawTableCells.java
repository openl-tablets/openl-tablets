package org.openl.studio.projects.service.tables.read;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * The cells of a table read as a matrix, and the styles they name.
 *
 * @param source the cells, row by row
 * @param styles the styles the cells and the pieces of their text name by index, or {@code null} for a read of the
 *               values alone
 * @author Yury Molchan
 */
public record RawTableCells(List<List<RawTableCell>> source, @Nullable List<RawTableCellStyle> styles) {
}
