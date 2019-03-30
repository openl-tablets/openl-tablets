package org.openl.rules.table.actions.style.font;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetBoldAction extends AUndoableCellAction {

    private boolean bold;

    public SetBoldAction(int col, int row, boolean bold, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.bold = bold;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        grid.setCellFontBold(getCol(), getRow(), bold);
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontBold(getCol(), getRow(), !bold);
    }

}
