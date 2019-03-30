package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * Action which resizes merged regions in some grid.
 * 
 * @author PUdalau
 */
public class UndoableResizeMergedRegionAction implements IUndoableGridTableAction {

    private IGridRegion initialRegion;
    private IGridRegion newRegion;

    public UndoableResizeMergedRegionAction(IGridRegion initialRegion, int numberOfRowsOrColumns, boolean isInsert,
            boolean isColumns) {
        this.initialRegion = initialRegion;
        int increase = isInsert ? numberOfRowsOrColumns : -numberOfRowsOrColumns;
        try {
            if (isColumns) {
                newRegion = new GridRegion(initialRegion.getTop(), initialRegion.getLeft(), initialRegion.getBottom(),
                        initialRegion.getRight() + increase);
            } else {
                newRegion = new GridRegion(initialRegion.getTop(), initialRegion.getLeft(), initialRegion.getBottom()
                        + increase, initialRegion.getRight());
            }
        } catch (IllegalArgumentException e) {
            // Wrong region created
            newRegion = null;
        }
        if(IGridRegion.Tool.width(newRegion) == 1 && IGridRegion.Tool.height(newRegion) == 1){
            newRegion = null;
        }
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        grid.removeMergedRegion(initialRegion);
        if (newRegion != null) {
            grid.addMergedRegion(newRegion);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        if (newRegion != null) {
            grid.removeMergedRegion(newRegion);
        }
        grid.addMergedRegion(initialRegion);
    }

}
