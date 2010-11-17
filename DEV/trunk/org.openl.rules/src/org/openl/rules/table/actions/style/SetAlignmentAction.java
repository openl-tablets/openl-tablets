package org.openl.rules.table.actions.style;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetAlignmentAction extends AUndoableCellAction {

    private int prevAlignment;
    private int newAlignment;

    public SetAlignmentAction(int col, int row, int alignment) {
        super(col, row);
        this.newAlignment = alignment;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        prevAlignment = grid.getCell(getCol(), getRow()).getStyle().getHorizontalAlignment();

        grid.setCellAlignment(getCol(), getRow(), newAlignment);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellAlignment(getCol(), getRow(), prevAlignment);
    }

}
