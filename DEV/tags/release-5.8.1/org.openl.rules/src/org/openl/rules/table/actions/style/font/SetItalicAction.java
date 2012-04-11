package org.openl.rules.table.actions.style.font;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetItalicAction extends AUndoableCellAction {

    private boolean italic;

    public SetItalicAction(int col, int row, boolean italic) {
        super(col, row);
        this.italic = italic;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        grid.setCellFontItalic(getCol(), getRow(), italic);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontItalic(getCol(), getRow(), !italic);
    }

}
