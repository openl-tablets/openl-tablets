package org.openl.rules.table.actions;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;

public class UndoableSetStyleAction extends AUndoableCellAction {

    private ICellStyle newStyle;

    public UndoableSetStyleAction(int col, int row, ICellStyle style) {
        super(col, row);
        this.newStyle = style;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        setPrevStyle(new CellStyle(grid.getCell(getCol(), getRow()).getStyle()));

        grid.setCellStyle(getCol(), getRow(), newStyle);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellStyle(getCol(), getRow(), getPrevStyle());
    }

}
