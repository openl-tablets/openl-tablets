package org.openl.rules.xls.merge.diff;

import org.apache.poi.ss.util.CellAddress;

/**
 * A cell of a merged table both revisions changed differently: its value or its comment.
 *
 * @param cell        the cell in the merged sheet
 * @param our         the cell in OUR revision
 * @param base        the cell in BASE revision
 * @param their       the cell in THEIR revision
 * @param ourSource   the content the cell takes when OUR revision is chosen for it
 * @param theirSource the content the cell takes when THEIR revision is chosen for it
 */
public record CellConflict(CellAddress cell,
                           CellAddress our,
                           CellAddress base,
                           CellAddress their,
                           CellSource ourSource,
                           CellSource theirSource) {
}
