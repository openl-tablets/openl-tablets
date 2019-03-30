package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * @author PUdalau
 */
public class MergeCellsAction implements IUndoableGridTableAction {

    private IGridRegion region;
    private List<IGridRegion> removedRegions;

    public MergeCellsAction(IGridRegion region) {
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
        for(IGridRegion regionToRemove :removedRegions){
            grid.removeMergedRegion(regionToRemove);
        }
        grid.addMergedRegion(region);
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.removeMergedRegion(region);
        for (IGridRegion mergedRegion : removedRegions) {
            grid.addMergedRegion(mergedRegion);
        }
    }

}
