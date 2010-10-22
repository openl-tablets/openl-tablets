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
public class UndoableCopyValueAction extends AUndoableCellAction {

    private int colFrom, rowFrom;

    private GridRegion toRestore, toRemove;

    public UndoableCopyValueAction(int colFrom, int rowFrom, int colTo, int rowTo) {
        super(colTo, rowTo);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        savePrevCell(grid);

        grid.copyCell(colFrom, rowFrom, getCol(), getRow());
        moveRegion(grid);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        if (toRemove != null) {
            grid.removeMergedRegion(toRemove);
        }
        if (toRestore != null) {
            grid.addMergedRegion(toRestore);
        }

        restorePrevCell(grid);
    }

    void moveRegion(IWritableGrid wgrid) {
        IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        IGridRegion rrTo = wgrid.getRegionStartingAt(getCol(), getRow());

        if (rrTo != null) {
            toRestore = new GridRegion(rrTo);
            wgrid.removeMergedRegion(toRestore);
        }

        if (rrFrom != null) {
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                    rrFrom.getLeft() + getCol() - colFrom, rrFrom.getBottom() + getRow() - rowFrom,
                    rrFrom.getRight() + getCol() - colFrom);
            wgrid.addMergedRegion(copyFrom);
            toRemove = copyFrom;
        }

    }

}
