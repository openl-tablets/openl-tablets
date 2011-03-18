package org.openl.rules.table.actions.style.font;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetBoldAction extends AUndoableCellAction {

    private boolean bold;

    public SetBoldAction(int col, int row, boolean bold) {
        super(col, row);
        this.bold = bold;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        grid.setCellFontBold(getCol(), getRow(), bold);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontBold(getCol(), getRow(), !bold);
    }

}
