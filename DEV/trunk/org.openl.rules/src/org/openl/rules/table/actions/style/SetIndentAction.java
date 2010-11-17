package org.openl.rules.table.actions.style;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetIndentAction extends AUndoableCellAction {

    private int prevIndent;
    private int newIndent;

    public SetIndentAction(int col, int row, int indent) {
        super(col, row);
        this.newIndent = indent;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        prevIndent = grid.getCell(getCol(), getRow()).getStyle().getIdent();

        grid.setCellIndent(getCol(), getRow(), newIndent);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellIndent(getCol(), getRow(), prevIndent);
    }

}
