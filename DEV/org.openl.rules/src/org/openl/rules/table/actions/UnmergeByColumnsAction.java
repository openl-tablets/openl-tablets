package org.openl.rules.table.actions;

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

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column < region.getRight(); column++) {
                var mergedRegion = grid.getRegionStartingAt(column, row);
                if (mergedRegion != null && IGridRegion.Tool.width(mergedRegion) > 1) {
                    grid.removeMergedRegion(mergedRegion);
                    for (var i = mergedRegion.getLeft(); i <= mergedRegion.getRight(); i++) {
                        grid.addMergedRegion(new GridRegion(mergedRegion.getTop(), i, mergedRegion.getBottom(), i));
                    }
                    column = mergedRegion.getRight();
                }
            }
        }
    }

}
