package org.openl.rules.table.actions;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

public class UndoableSaveValueAction extends AUndoableCellAction {

    public UndoableSaveValueAction(int col, int row) {
        super(col, row);
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        savePrevCell(grid);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        restorePrevCell(grid);
    }

}
