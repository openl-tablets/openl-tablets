package org.openl.rules.table.actions;

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

    private IGridRegion toRestore;
    private IGridRegion toRemove;

    public UndoableShiftValueAction(int colFrom, int rowFrom, int colTo, int rowTo) {
        super(colTo, rowTo);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    // Save value from initial cell -> move region -> set value to destination
    // cell
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        IGridRegion rrFrom = grid.getRegionStartingAt(colFrom, rowFrom);
        setPrevValue(grid.getCell(colFrom, rowFrom).getObjectValue());
        setPrevFormula(grid.getCell(colFrom, rowFrom).getFormula());
        setPrevStyle(grid.getCell(colFrom, rowFrom).getStyle());
        if (rrFrom != null) {
            toRestore = rrFrom;
            grid.removeMergedRegion(rrFrom);
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + getRow() - rowFrom, rrFrom.getLeft() + getCol()
                    - colFrom, rrFrom.getBottom() + getRow() - rowFrom, rrFrom.getRight() + getCol() - colFrom);
            grid.addMergedRegion(copyFrom);
            toRemove = copyFrom;
        }
        grid.setCellFormula(getCol(), getRow(), getPrevFormula());
        grid.setCellValue(getCol(), getRow(), getPrevValue());
        grid.setCellStyle(getCol(), getRow(), getPrevStyle());
    }

    // Save value from destination cell -> move region back -> set value to
    // initial cell
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        setPrevValue(grid.getCell(getCol(), getRow()).getObjectValue());
        setPrevFormula(grid.getCell(getCol(), getRow()).getFormula());
        setPrevStyle(grid.getCell(getCol(), getRow()).getStyle());
        if (toRemove != null) {
            grid.removeMergedRegion(toRemove);
            grid.addMergedRegion(toRestore);
        }
        grid.setCellFormula(colFrom, rowFrom, getPrevFormula());
        grid.setCellValue(colFrom, rowFrom, getPrevValue());
        grid.setCellStyle(colFrom, rowFrom, getPrevStyle());
    }
}
