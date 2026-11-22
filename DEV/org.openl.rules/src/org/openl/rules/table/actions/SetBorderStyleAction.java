package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

public class SetBorderStyleAction extends AUndoableCellAction {
    private final int col;
    private final int row;

    private final ICellStyle newCellStyle;

    public SetBorderStyleAction(int col, int row, ICellStyle newCellStyle, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.col = col;
        this.row = row;
        this.newCellStyle = newCellStyle;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.clearCell(getCol(), getRow());

        grid.setCellBorderStyle(col, row, newCellStyle);
    }

}
