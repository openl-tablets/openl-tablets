/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 *
 */
public class UndoableClearAction extends AUndoableCellAction {

    private GridRegion toRestore;

    public UndoableClearAction(int col, int row) {
        super(col, row);
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        savePrevCell(grid);

        grid.clearCell(getCol(), getRow());
        clearRegion(grid);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        if (toRestore != null) {
            grid.addMergedRegion(toRestore);
        }

        restorePrevCell(grid);
    }

    void clearRegion(IWritableGrid grid) {
        IGridRegion rrTo = grid.getRegionStartingAt(getCol(), getRow());

        if (rrTo == null) {
            return;
        }

        toRestore = new GridRegion(rrTo);
        grid.removeMergedRegion(toRestore);
    }

}
