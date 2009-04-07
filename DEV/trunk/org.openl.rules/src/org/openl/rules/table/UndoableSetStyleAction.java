package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;

public class UndoableSetStyleAction extends AUndoableCellAction {
    private ICellStyle style;

    public UndoableSetStyleAction(int col, int row, ICellStyle style) {
        super(col, row);
        this.style = style;
    }

    @Override
    public void doDirectChange(IWritableGrid wgrid) {
        wgrid.setCellStyle(col, row, style);
    }
}
