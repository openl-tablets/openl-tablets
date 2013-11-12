package org.openl.rules.table.actions.style.font;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetUnderlineAction extends AUndoableCellAction {

    private boolean underlined;

    public SetUnderlineAction(int col, int row, boolean underlined) {
        super(col, row);
        this.underlined = underlined;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        grid.setCellFontUnderline(getCol(), getRow(), underlined);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontUnderline(getCol(), getRow(), !underlined);
    }

}
