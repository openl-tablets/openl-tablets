package org.openl.rules.table.actions.style.font;

import java.util.function.Consumer;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

public class SetColorAction extends AUndoableCellAction {

    /** Sets the colour on the grid of the table. */
    private final Consumer<IWritableGrid> colour;

    public SetColorAction(int col, int row, short[] color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.colour = grid -> grid.setCellFontColor(col, row, color);
    }

    /**
     * Colours the text of a cell with a theme colour of Excel, made lighter or darker.
     *
     * @see IWritableGrid#setCellFontColor(int, int, ThemedColor)
     */
    public SetColorAction(int col, int row, ThemedColor color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.colour = grid -> grid.setCellFontColor(col, row, color);
    }

    @Override
    public void doAction(IGridTable table) {
        colour.accept((IWritableGrid) table.getGrid());
    }

}
