package org.openl.rules.table.actions.style;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetIndentAction extends AUndoableCellAction {

    private int prevIndent;
    private final int newIndent;

    public SetIndentAction(int col, int row, int indent, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newIndent = indent;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();

        var style = grid.getCell(getCol(), getRow()).getStyle();
        prevIndent = style != null ? style.getIndent() : 0;

        grid.setCellIndent(getCol(), getRow(), newIndent);
    }

    @Override
    public void undoAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.setCellIndent(getCol(), getRow(), prevIndent);
    }

}
