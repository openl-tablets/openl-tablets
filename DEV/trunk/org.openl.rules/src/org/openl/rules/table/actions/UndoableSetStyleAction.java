package org.openl.rules.table.actions;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;

public class UndoableSetStyleAction extends AUndoableCellAction {

    private ICellStyle prevStyle;
    private ICellStyle newStyle;

    public UndoableSetStyleAction(int col, int row, ICellStyle style) {
        super(col, row);
        this.newStyle = style;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        prevStyle = new CellStyle(grid.getCell(col, row).getStyle());
        grid.setCellStyle(col, row, newStyle);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellStyle(col, row, prevStyle);
    }

}
