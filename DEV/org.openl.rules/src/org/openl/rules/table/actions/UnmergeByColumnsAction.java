package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author PUdalau
 */
@RequiredArgsConstructor
public class UnmergeByColumnsAction implements IUndoableGridTableAction {

    private final IGridRegion region;
    private List<IGridRegion> createdRegions;
    private List<IGridRegion> removedRegions;

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        createdRegions = new ArrayList<>();
        removedRegions = new ArrayList<>();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column < region.getRight(); column++) {
                var mergedRegion = grid.getRegionStartingAt(column, row);
                if (mergedRegion != null && IGridRegion.Tool.width(mergedRegion) > 1) {
                    removedRegions.add(mergedRegion);
                    grid.removeMergedRegion(mergedRegion);
                    for (var i = mergedRegion.getLeft(); i <= mergedRegion.getRight(); i++) {
                        var newRegion = new GridRegion(mergedRegion.getTop(), i, mergedRegion.getBottom(), i);
                        grid.addMergedRegion(newRegion);
                        createdRegions.add(newRegion);
                    }
                    column = mergedRegion.getRight();
                }
            }
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        for (IGridRegion region : createdRegions) {
            grid.removeMergedRegion(region);
        }
        for (IGridRegion region : removedRegions) {
            grid.addMergedRegion(region);
        }
    }

}
