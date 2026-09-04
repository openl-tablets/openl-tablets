package org.openl.rules.table.actions.style.font;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;

public class SetColorAction extends AUndoableCellAction {

    private short[] prevColor;
    private final short[] newColor;

    public SetColorAction(int col, int row, short[] color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newColor = color;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();

        var font = grid.getCell(getCol(), getRow()).getFont();
        prevColor = font != null ? font.getFontColor() : null;

        grid.setCellFontColor(getCol(), getRow(), newColor);
    }

    @Override
    public void undoAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.setCellFontColor(getCol(), getRow(), prevColor);
    }

}
