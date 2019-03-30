package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

public class SetBorderStyleAction extends AUndoableCellAction {
    private int col;
    private int row;

    private boolean clearCell = true;

    private ICellStyle newCellStyle;

    public SetBorderStyleAction(int col, int row, ICellStyle newCellStyle, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.col = col;
        this.row = row;
        this.newCellStyle = newCellStyle;
    }

    public SetBorderStyleAction(int col,
            int row,
            ICellStyle newCellStyle,
            boolean clearCell,
            MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.col = col;
        this.row = row;
        this.newCellStyle = newCellStyle;
        this.clearCell = clearCell;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        savePrevCell(grid);

        if (clearCell) {
            grid.clearCell(getCol(), getRow());
        }

        grid.setCellBorderStyle(col, row, newCellStyle);
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        restorePrevCell(grid);
    }
}
