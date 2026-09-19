package org.openl.rules.table.actions;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * Removes every merged region whose origin (top-left cell) lies within the given region.
 * <p>
 * Use it to drop all merges of an area before re-applying merges from a fresh source, so that merges removed from
 * the source no longer linger in the grid. {@link MergeCellsAction} extends this action: it clears the overlapping
 * merges the same way and then adds its own region as a new merge.
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class RemoveMergedRegionsAction implements IUndoableGridTableAction {

    protected final IGridRegion region;

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        var removedRegions = new ArrayList<IGridRegion>();
        var nregions = grid.getNumberOfMergedRegions();
        for (var i = 0; i < nregions; i++) {
            var reg = grid.getMergedRegion(i);
            if (IGridRegion.Tool.contains(region, reg.getLeft(), reg.getTop())) {
                removedRegions.add(reg);
            }
        }
        for (IGridRegion regionToRemove : removedRegions) {
            grid.removeMergedRegion(regionToRemove);
        }
    }

}
