package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;

/**
 * Action to remove columns from the table. If the column is merged, then all columns in the merged cell will be removed.
 */
public class UndoableRemoveMergedColumnsAction extends UndoableRemoveColumnsAction {

    private final int row;

    public UndoableRemoveMergedColumnsAction(int nCols, int startCol, int row, MetaInfoWriter metaInfoWriter) {
        super(nCols, startCol, metaInfoWriter);
        this.row = row;
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        int cellWidth = getOriginalTable(table).getCell(startCol, row).getWidth();
        int numberToInsert = nCols;
        if (cellWidth > 1) { // merged cell
            numberToInsert += cellWidth - 1;
        }
        return numberToInsert;
    }

}
