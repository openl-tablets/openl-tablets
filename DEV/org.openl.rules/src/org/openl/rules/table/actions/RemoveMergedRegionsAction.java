package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * Removes every merged region whose origin (top-left cell) lies within the given region; undo restores them.
 * <p>
 * Use it to drop all merges of an area before re-applying merges from a fresh source, so that merges removed from
 * the source no longer linger in the grid. {@link MergeCellsAction} extends this action: it clears the overlapping
 * merges the same way and then adds its own region as a new merge.
 *
 * @author Vladyslav Pikus
 */
public class RemoveMergedRegionsAction implements IUndoableGridTableAction {

    protected final IGridRegion region;
    private List<IGridRegion> removedRegions;

    public RemoveMergedRegionsAction(IGridRegion region) {
        this.region = region;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        removedRegions = new ArrayList<>();
        int nregions = grid.getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            IGridRegion reg = grid.getMergedRegion(i);
            if (IGridRegion.Tool.contains(region, reg.getLeft(), reg.getTop())) {
                removedRegions.add(reg);
            }
        }
        for (IGridRegion regionToRemove : removedRegions) {
            grid.removeMergedRegion(regionToRemove);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        for (IGridRegion mergedRegion : removedRegions) {
            grid.addMergedRegion(mergedRegion);
        }
    }

}
