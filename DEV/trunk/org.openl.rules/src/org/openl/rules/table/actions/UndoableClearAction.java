/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
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

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        ICell prevCell = grid.getCell(col, row);
        prevCellValue = prevCell.getObjectValue();
        prevCellStyle = prevCell.getStyle();

        grid.clearCell(col, row);
        clearRegion(grid);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        if (toRestore != null) {
            grid.addMergedRegion(toRestore);
        }

        grid.createCell(col, row, prevCellValue, prevCellStyle);
    }

    void clearRegion(IWritableGrid grid) {
        IGridRegion rrTo = grid.getRegionStartingAt(col, row);

        if (rrTo == null) {
            return;
        }

        toRestore = new GridRegion(rrTo);
        grid.removeMergedRegion(toRestore);
    }

}
