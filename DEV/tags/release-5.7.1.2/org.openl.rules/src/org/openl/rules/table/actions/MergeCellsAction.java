package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IWritableGrid;

/**
 * @author PUdalau
 */
public class MergeCellsAction implements IUndoableGridAction {

    private IGridRegion region;
    private List<IGridRegion> removedRegions;

    public MergeCellsAction(IGridRegion region) {
        this.region = region;
    }

    public void doAction(IWritableGrid grid, IUndoGrid undo) {
        removedRegions = new ArrayList<IGridRegion>();
        int nregions = grid.getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            IGridRegion reg = grid.getMergedRegion(i);
            if (IGridRegion.Tool.contains(region, reg.getLeft(), reg.getTop())) {
                removedRegions.add(reg);
            }
        }
        for(IGridRegion regionToRemove :removedRegions){
            grid.removeMergedRegion(regionToRemove);
        }
        grid.addMergedRegion(region);
    }

    public void undoAction(IWritableGrid grid, IUndoGrid undo) {
        grid.removeMergedRegion(region);
        for (IGridRegion mergedRegion : removedRegions) {
            grid.addMergedRegion(mergedRegion);
        }
    }

}
