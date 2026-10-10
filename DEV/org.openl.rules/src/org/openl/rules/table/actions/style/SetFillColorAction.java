package org.openl.rules.table.actions.style;

import java.util.function.Consumer;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

public class SetFillColorAction extends AUndoableCellAction {

    /** Sets the colour on the grid of the table. */
    private final Consumer<IWritableGrid> colour;

    public SetFillColorAction(int col, int row, short[] color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.colour = grid -> grid.setCellFillColor(col, row, color);
    }

    /**
     * Fills a cell with a theme colour of Excel, made lighter or darker.
     *
     * @see IWritableGrid#setCellFillColor(int, int, ThemedColor)
     */
    public SetFillColorAction(int col, int row, ThemedColor color, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.colour = grid -> grid.setCellFillColor(col, row, color);
    }

    @Override
    public void doAction(IGridTable table) {
        colour.accept((IWritableGrid) table.getGrid());
    }

}
