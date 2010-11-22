package org.openl.rules.table.actions.style.font;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetColorAction extends AUndoableCellAction {

    private short[] prevColor;
    private short[] newColor;

    public SetColorAction(int col, int row, short[] color) {
        super(col, row);
        this.newColor = color;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        prevColor = grid.getCell(getCol(), getRow()).getFont().getFontColor();

        grid.setCellFontColor(getCol(), getRow(), newColor);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontColor(getCol(), getRow(), prevColor);
    }

}
