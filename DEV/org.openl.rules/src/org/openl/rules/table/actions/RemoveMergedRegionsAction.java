package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.table.GridRegionUtils;
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

    /** The merged regions the action removed, empty until it is done. */
    @Getter
    private List<IGridRegion> removedRegions = List.of();

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        var removed = new ArrayList<IGridRegion>();
        var nregions = grid.getNumberOfMergedRegions();
        for (var i = 0; i < nregions; i++) {
            var reg = grid.getMergedRegion(i);
            if (GridRegionUtils.contains(region, reg.getLeft(), reg.getTop())) {
                removed.add(reg);
            }
        }
        for (IGridRegion regionToRemove : removed) {
            grid.removeMergedRegion(regionToRemove);
        }
        removedRegions = List.copyOf(removed);
    }

}
