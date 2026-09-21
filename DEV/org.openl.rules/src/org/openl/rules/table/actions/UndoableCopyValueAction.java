package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 */
public class UndoableCopyValueAction extends AUndoableCellAction {

    private final int colFrom;
    private final int rowFrom;

    public UndoableCopyValueAction(int colFrom, int rowFrom, int colTo, int rowTo, MetaInfoWriter metaInfoWriter) {
        super(colTo, rowTo, metaInfoWriter);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        grid.copyCell(colFrom, rowFrom, getCol(), getRow());
        var metaInfo = metaInfoWriter.getMetaInfo(rowFrom, colFrom);
        if (metaInfo != null && metaInfo.getUsedNodes() != null) {
            // Remove NodeUsage for a new cell because it can contain another string so NodeUsage will be incorrect.
            metaInfo = new CellMetaInfo(metaInfo.getDataType(), metaInfo.isMultiValue());
        }
        metaInfoWriter.setMetaInfo(getRow(), getCol(), metaInfo);
        moveRegion(grid);
    }

    private void moveRegion(IWritableGrid wgrid) {
        var rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        var rrTo = wgrid.getRegionStartingAt(getCol(), getRow());

        if (rrTo != null) {
            wgrid.removeMergedRegion(new GridRegion(rrTo));
        }

        if (rrFrom != null) {
            wgrid.addMergedRegion(new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                    rrFrom.getLeft() + getCol() - colFrom,
                    rrFrom.getBottom() + getRow() - rowFrom,
                    rrFrom.getRight() + getCol() - colFrom));
        }
    }

}
