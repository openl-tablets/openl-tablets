package org.openl.rules.table.actions.style.font;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.ui.ICellFont;

public class SetColorAction extends AUndoableCellAction {

    private short[] prevColor;
    private short[] newColor;

    public SetColorAction(int col, int row, short[] color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newColor = color;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICellFont font = grid.getCell(getCol(), getRow()).getFont();
        prevColor = font != null ? font.getFontColor() : null;

        grid.setCellFontColor(getCol(), getRow(), newColor);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.setCellFontColor(getCol(), getRow(), prevColor);
    }

}
