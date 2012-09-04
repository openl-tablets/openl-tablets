package org.openl.rules.table.actions;

import org.openl.rules.table.Cell;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;

public class SetBorderStyleAction extends AUndoableCellAction {
    private GridRegion toRestore;
    int col;
    int row;
    int tableColNum;
    int propSize;
    int leftBorderColumn;
    
    ICellStyle newCellStyle;

    public SetBorderStyleAction(int col, int row, ICellStyle newCellStyle) {
        super(col, row);
        this.col = col;
        this.row = row;
        this.newCellStyle = newCellStyle;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
       
        savePrevCell(grid);
        grid.clearCell(getCol(), getRow());
        grid.setCellBorderStyle(col, row, newCellStyle);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        if (toRestore != null) {
            grid.addMergedRegion(toRestore);
        }

        restorePrevCell(grid);
    }
}
