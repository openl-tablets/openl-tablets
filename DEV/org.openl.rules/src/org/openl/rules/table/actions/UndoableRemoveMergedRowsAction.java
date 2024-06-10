package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;

/**
 * Action to remove rows from the table. If the row is merged, then all rows in the merged cell will be removed.
 */
public class UndoableRemoveMergedRowsAction extends UndoableRemoveRowsAction {

    private final int col;

    public UndoableRemoveMergedRowsAction(int nRows, int startRow, int col, MetaInfoWriter metaInfoWriter) {
        super(nRows, startRow, metaInfoWriter);
        this.col = col;
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        int cellHeight = getOriginalTable(table).getCell(col, startRow).getHeight();
        int numberToRemove = nRows;
        if (cellHeight > 1) { // merged cell
            numberToRemove += cellHeight - 1;
        }
        return numberToRemove;
    }

}
