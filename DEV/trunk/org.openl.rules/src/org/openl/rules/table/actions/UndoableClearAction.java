/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */
public class UndoableClearAction extends AUndoableCellAction {

    GridRegion toRestore;

    Object prevCellValue;
    ICellStyle prevCellStyle;

    public UndoableClearAction(int col, int row) {
        super(col, row);
    }

    public void doAction(IWritableGrid wgrid) {
        ICell prevCell = wgrid.getCell(col, row);
        prevCellValue = prevCell.getObjectValue();
        prevCellStyle = prevCell.getStyle();

        wgrid.clearCell(col, row);
        clearRegion(wgrid);
    }

    public void undoAction(IWritableGrid wgrid) {
        if (toRestore != null) {
            wgrid.addMergedRegion(toRestore);
        }

        wgrid.createCell(col, row, prevCellValue, prevCellStyle);
    }

    void clearRegion(IWritableGrid wgrid) {
        IGridRegion rrTo = wgrid.getRegionStartingAt(col, row);

        if (rrTo == null) {
            return;
        }

        toRestore = new GridRegion(rrTo);
        wgrid.removeMergedRegion(toRestore);
    }

}
