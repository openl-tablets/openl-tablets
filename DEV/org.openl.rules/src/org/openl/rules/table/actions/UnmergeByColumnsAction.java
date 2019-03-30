package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author PUdalau
 */
public class UnmergeByColumnsAction implements IUndoableGridTableAction {

    private IGridRegion region;
    private List<IGridRegion> createdRegions;
    private List<IGridRegion> removedRegions;

    public UnmergeByColumnsAction(IGridRegion region) {
        this.region = region;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        createdRegions = new ArrayList<>();
        removedRegions = new ArrayList<>();
        for (int row = region.getTop(); row <= region.getBottom(); row++) {
            for (int column = region.getLeft(); column < region.getRight(); column++) {
                IGridRegion mergedRegion = grid.getRegionStartingAt(column, row);
                if (mergedRegion != null && IGridRegion.Tool.width(mergedRegion) > 1) {
                    removedRegions.add(mergedRegion);
                    grid.removeMergedRegion(mergedRegion);
                    for (int i = mergedRegion.getLeft(); i <= mergedRegion.getRight(); i++) {
                        IGridRegion newRegion = new GridRegion(mergedRegion.getTop(), i, mergedRegion.getBottom(), i);
                        grid.addMergedRegion(newRegion);
                        createdRegions.add(newRegion);
                    }
                    column = mergedRegion.getRight();
                }
            }
        }
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        for (IGridRegion region : createdRegions) {
            grid.removeMergedRegion(region);
        }
        for (IGridRegion region : removedRegions) {
            grid.addMergedRegion(region);
        }
    }

}
