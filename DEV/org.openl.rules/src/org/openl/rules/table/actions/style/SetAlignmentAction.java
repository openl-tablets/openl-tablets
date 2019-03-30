package org.openl.rules.table.actions.style;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.ui.ICellStyle;

public class SetAlignmentAction extends AUndoableCellAction {

    private HorizontalAlignment prevAlignment;
    private HorizontalAlignment newAlignment;

    public SetAlignmentAction(int col, int row, HorizontalAlignment alignment, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newAlignment = alignment;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICellStyle style = grid.getCell(getCol(), getRow()).getStyle();
        prevAlignment = style != null ? style.getHorizontalAlignment() : HorizontalAlignment.GENERAL;

        grid.setCellAlignment(getCol(), getRow(), newAlignment);
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellAlignment(getCol(), getRow(), prevAlignment);
    }

}
