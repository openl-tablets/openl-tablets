package org.openl.rules.table.actions.style;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetIndentAction extends AUndoableCellAction {

    private final int newIndent;

    public SetIndentAction(int col, int row, int indent, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newIndent = indent;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.setCellIndent(getCol(), getRow(), newIndent);
    }

}
