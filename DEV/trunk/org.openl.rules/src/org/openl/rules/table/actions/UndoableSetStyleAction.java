package org.openl.rules.table.actions;

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

    public void doAction(IWritableGrid wgrid) {
        prevStyle = new CellStyle(wgrid.getCell(col, row).getStyle());
        wgrid.setCellStyle(col, row, newStyle);
    }

    public void undoAction(IWritableGrid wgrid) {
        wgrid.setCellStyle(col, row, prevStyle);
    }

}
