package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 *
 */
public class UndoableCopyValueAction extends AUndoableCellAction {

    private int colFrom;
    private int rowFrom;

    private GridRegion toRestore;
    private GridRegion toRemove;

    public UndoableCopyValueAction(int colFrom, int rowFrom, int colTo, int rowTo, MetaInfoWriter metaInfoWriter) {
        super(colTo, rowTo, metaInfoWriter);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        savePrevCell(grid);

        grid.copyCell(colFrom, rowFrom, getCol(), getRow());
        CellMetaInfo metaInfo = metaInfoWriter.getMetaInfo(rowFrom, colFrom);
        if (metaInfo != null && metaInfo.getUsedNodes() != null) {
            // Remove NodeUsage for a new cell because it can contain another string so NodeUsage will be incorrect.
            metaInfo = new CellMetaInfo(metaInfo.getDataType(), metaInfo.isMultiValue());
        }
        metaInfoWriter.setMetaInfo(getRow(), getCol(), metaInfo);
        moveRegion(grid);
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        if (toRemove != null) {
            grid.removeMergedRegion(toRemove);
        }
        if (toRestore != null) {
            grid.addMergedRegion(toRestore);
        }

        restorePrevCell(grid);
    }

    private void moveRegion(IWritableGrid wgrid) {
        IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
        IGridRegion rrTo = wgrid.getRegionStartingAt(getCol(), getRow());

        if (rrTo != null) {
            toRestore = new GridRegion(rrTo);
            wgrid.removeMergedRegion(toRestore);
        }

        if (rrFrom != null) {
            GridRegion copyFrom = new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                rrFrom.getLeft() + getCol() - colFrom,
                rrFrom.getBottom() + getRow() - rowFrom,
                rrFrom.getRight() + getCol() - colFrom);
            wgrid.addMergedRegion(copyFrom);
            toRemove = copyFrom;
        }

    }

}
