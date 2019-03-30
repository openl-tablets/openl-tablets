package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
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

    public UndoableShiftValueAction(int colFrom, int rowFrom, int colTo, int rowTo, MetaInfoWriter metaInfoWriter) {
        super(colTo, rowTo, metaInfoWriter);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    // Save value from initial cell -> move region -> set value to destination
    // cell
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        IGridRegion rrFrom = grid.getRegionStartingAt(colFrom, rowFrom);
        
        ICell cell = grid.getCell(colFrom, rowFrom);

        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevStyle(cell.getStyle());
        setPrevComment(cell.getComment());
        setPrevMetaInfo(metaInfoWriter.getMetaInfo(rowFrom, colFrom));

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
        grid.setCellComment(getCol(), getRow(), getPrevComment());
        metaInfoWriter.setMetaInfo(getRow(), getCol(), getPrevMetaInfo());

        ICell newCell = grid.getCell(getCol(), getRow());
        if (cell.getType() == IGrid.CELL_TYPE_STRING && newCell.getType() == IGrid.CELL_TYPE_FORMULA) {
            grid.setCellStringValue(getCol(), getRow(), cell.getObjectValue().toString());
        }
    }

    // Save value from destination cell -> move region back -> set value to
    // initial cell
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICell cell = grid.getCell(getCol(), getRow());

        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevStyle(cell.getStyle());
        setPrevComment(cell.getComment());

        if (toRemove != null) {
            grid.removeMergedRegion(toRemove);
            grid.addMergedRegion(toRestore);
        }

        grid.setCellFormula(colFrom, rowFrom, getPrevFormula());
        grid.setCellValue(colFrom, rowFrom, getPrevValue());
        grid.setCellStyle(colFrom, rowFrom, getPrevStyle());
        grid.setCellComment(colFrom, rowFrom, getPrevComment());
        metaInfoWriter.setMetaInfo(rowFrom, colFrom, getPrevMetaInfo());
    }

}
