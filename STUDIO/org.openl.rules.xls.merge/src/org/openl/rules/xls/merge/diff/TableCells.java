package org.openl.rules.xls.merge.diff;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * A conflicting table merged cell by cell: what each cell of the merged table takes, and the cells both revisions
 * changed differently, which are left to a {@link CellResolution}.
 *
 * @param place     the place of the merged table in the merged sheet
 * @param cells     every cell of the merged table by its address in the merged sheet, with the content it takes; a
 *                  conflicting cell is listed with the content it takes from OUR revision
 * @param regions   the merged cells of the merged table, at their places in the merged sheet
 * @param conflicts the cells both revisions changed differently, row by row
 * @param changed   the cells whose value or comment one revision alone changed, the cells of a line one revision alone
 *                  has included, row by row: the merge takes them from that revision by itself
 */
public record TableCells(CellRangeAddress place,
                         Map<CellAddress, CellSource> cells,
                         List<CellRangeAddress> regions,
                         List<CellConflict> conflicts,
                         List<CellAddress> changed) {

    public TableCells {
        cells = Map.copyOf(cells);
        regions = List.copyOf(regions);
        conflicts = List.copyOf(conflicts);
        changed = List.copyOf(changed);
    }
}
