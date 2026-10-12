package org.openl.rules.xls.merge.diff;

import java.util.Set;

import org.apache.poi.ss.util.CellAddress;
import org.jspecify.annotations.Nullable;

/**
 * Where a cell of a merged sheet takes its content from.
 *
 * <p>The cell starts as a copy of a cell of OUR revision, and then takes some parts from a cell of THEIR revision.
 * Both cells may stand at other addresses than the merged cell, when a revision moved the content of the sheet.
 *
 * <p>A cell with no cell of OUR revision to start from starts empty. A part THEIR revision has not got is cleared.
 *
 * @param our   the cell of OUR revision the merged cell starts from, or {@code null} to start empty
 * @param their the cell of THEIR revision the parts are taken from, or {@code null} when no part is taken
 * @param parts the parts taken from THEIR revision
 */
public record CellSource(@Nullable CellAddress our, @Nullable CellAddress their, Set<CellPart> parts) {
}
