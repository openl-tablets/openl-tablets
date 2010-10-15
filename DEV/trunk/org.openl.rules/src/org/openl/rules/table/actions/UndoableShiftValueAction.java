package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * Shift cell with merged region.
 * 
 * @author PUdalau
 */
public class UndoableShiftValueAction extends AUndoableCellAction {

    private int colFrom, rowFrom;

    private Object prevCellValue;
    private ICellStyle prevCellStyle;

    private List<IGridRegion> toRestore = new ArrayList<IGridRegion>();
    private List<IGridRegion> toRemove = new ArrayList<IGridRegion>();

    public UndoableShiftValueAction(int colFrom, int rowFrom, int colTo, int rowTo) {
        super(colTo, rowTo);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    public void doAction(IWritableGrid wgrid) {
        ICell prevCell = wgrid.getCell(col, row);
        prevCellValue = prevCell.getObjectValue();
        prevCellStyle = prevCell.getStyle();

        wgrid.copyCell(colFrom, rowFrom, col, row);
        moveRegion(wgrid);
    }

    public void undoAction(IWritableGrid wgrid) {
        for (IGridRegion region : toRemove) {
            wgrid.removeMergedRegion(region);
        }
        for (IGridRegion region : toRestore) {
            wgrid.addMergedRegion(region);
        }
        if (prevCellValue != null) {
            wgrid.createCell(col, row, prevCellValue, prevCellStyle);
        } else {
            wgrid.clearCell(col, row);
        }
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

}
