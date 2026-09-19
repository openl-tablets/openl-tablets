package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 */
public class UndoableClearAction extends AUndoableCellAction {

    public UndoableClearAction(int col, int row, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.clearCell(getCol(), getRow());
        metaInfoWriter.setMetaInfo(getRow(), getCol(), null);
        clearRegion(grid);
    }

    private void clearRegion(IWritableGrid grid) {
        var rrTo = grid.getRegionStartingAt(getCol(), getRow());

        if (rrTo == null) {
            return;
        }

        grid.removeMergedRegion(new GridRegion(rrTo));
    }

}
