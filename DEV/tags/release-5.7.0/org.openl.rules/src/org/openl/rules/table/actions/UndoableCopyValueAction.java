/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IUndoGrid;
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

    @Override
    public void doDirectChange(IWritableGrid wgrid) {
        wgrid.copyCell(colFrom, rowFrom, col, row);
        moveRegion(wgrid);
    }

    void moveRegion(IWritableGrid wgrid) {
        IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        IGridRegion rrTo = wgrid.getRegionStartingAt(col, row);

        if (rrTo != null) {
            toRestore = new GridRegion(rrTo);
            wgrid.removeMergedRegion(toRestore);
        }

        if (rrFrom != null) {
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + row - rowFrom, rrFrom.getLeft() + col - colFrom,
                    rrFrom.getBottom() + row - rowFrom, rrFrom.getRight() + col - colFrom);
            wgrid.addMergedRegion(copyFrom);
            toRemove = copyFrom;
        }

    }

    @Override
    public void restore(IWritableGrid wgrid, IUndoGrid undo) {

        if (toRemove != null) {
            wgrid.removeMergedRegion(toRemove);
        }
        if (toRestore != null) {
            wgrid.addMergedRegion(toRestore);
        }
        super.restore(wgrid, undo);
    }

}
