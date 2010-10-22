package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
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

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        savePrevCell(grid);

        grid.copyCell(colFrom, rowFrom, getCol(), getRow());
        moveRegion(grid);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        for (IGridRegion region : toRemove) {
            grid.removeMergedRegion(region);
        }
        for (IGridRegion region : toRestore) {
            grid.addMergedRegion(region);
        }

        restorePrevCell(grid);
    }

    void moveRegion(IWritableGrid wgrid) {
        toRemove.clear();
        toRestore.clear();
        IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        IGridRegion rrTo = wgrid.getRegionStartingAt(getCol(), getRow());

        if (rrTo != null) {
            GridRegion removedRegion = new GridRegion(rrTo);
            toRestore.add(removedRegion);
            wgrid.removeMergedRegion(removedRegion);
        }

        if (rrFrom != null) {
            toRestore.add(rrFrom);
            wgrid.removeMergedRegion(rrFrom);
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                    rrFrom.getLeft() + getCol() - colFrom, rrFrom.getBottom() + getRow() - rowFrom,
                    rrFrom.getRight() + getCol() - colFrom);
            wgrid.addMergedRegion(copyFrom);
            toRemove.add(copyFrom);
        }

    }

}
