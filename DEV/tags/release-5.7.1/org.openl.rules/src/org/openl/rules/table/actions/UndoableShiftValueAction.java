package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IWritableGrid;

/**
 * Shift cell with merged region.
 * 
 * @author PUdalau
 */
public class UndoableShiftValueAction extends AUndoableCellAction {
    private int colFrom, rowFrom;

    private List<IGridRegion> toRestore = new ArrayList<IGridRegion>();
    private List<IGridRegion> toRemove = new ArrayList<IGridRegion>();

    public UndoableShiftValueAction(int colFrom, int rowFrom, int colTo, int rowTo) {
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
        toRemove.clear();
        toRestore.clear();
        IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        IGridRegion rrTo = wgrid.getRegionStartingAt(col, row);

        if (rrTo != null) {
            GridRegion removedRegion = new GridRegion(rrTo);
            toRestore.add(removedRegion);
            wgrid.removeMergedRegion(removedRegion);
        }

        if (rrFrom != null) {
            toRestore.add(rrFrom);
            wgrid.removeMergedRegion(rrFrom);
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + row - rowFrom, rrFrom.getLeft() + col - colFrom,
                    rrFrom.getBottom() + row - rowFrom, rrFrom.getRight() + col - colFrom);
            wgrid.addMergedRegion(copyFrom);
            toRemove.add(copyFrom);
        }

    }

    @Override
    public void restore(IWritableGrid wgrid, IUndoGrid undo) {
        for (IGridRegion region : toRemove) {
            wgrid.removeMergedRegion(region);
        }
        for (IGridRegion region : toRestore) {
            wgrid.addMergedRegion(region);
        }
        super.restore(wgrid, undo);
    }
}
