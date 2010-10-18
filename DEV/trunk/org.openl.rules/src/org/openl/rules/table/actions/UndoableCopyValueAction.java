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
public class UndoableCopyValueAction extends AUndoableCellAction {

    private int colFrom, rowFrom;

    private Object prevCellValue;
    private ICellStyle prevCellStyle;

    private GridRegion toRestore, toRemove;

    public UndoableCopyValueAction(int colFrom, int rowFrom, int colTo, int rowTo) {
        super(colTo, rowTo);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        ICell prevCell = grid.getCell(col, row);
        prevCellValue = prevCell.getObjectValue();
        prevCellStyle = prevCell.getStyle();

        grid.copyCell(colFrom, rowFrom, col, row);
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

        if (prevCellValue != null) {
            grid.createCell(col, row, prevCellValue, prevCellStyle);
        } else {
            grid.clearCell(col, row);
        }
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

}
