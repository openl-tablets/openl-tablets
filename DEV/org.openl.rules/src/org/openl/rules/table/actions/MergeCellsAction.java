package org.openl.rules.table.actions;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * Merges a region of cells into a single merged region.
 * <p>
 * Existing merges whose origin falls inside the new region are dropped first (inherited from
 * {@link RemoveMergedRegionsAction}); then the region is added as a merge. Undo reverses both steps.
 *
 * @author PUdalau
 */
public class MergeCellsAction extends RemoveMergedRegionsAction {

    public MergeCellsAction(IGridRegion region) {
        super(region);
    }

    @Override
    public void doAction(IGridTable table) {
        super.doAction(table);
        ((IWritableGrid) table.getGrid()).addMergedRegion(region);
    }

    @Override
    public void undoAction(IGridTable table) {
        ((IWritableGrid) table.getGrid()).removeMergedRegion(region);
        super.undoAction(table);
    }

}
