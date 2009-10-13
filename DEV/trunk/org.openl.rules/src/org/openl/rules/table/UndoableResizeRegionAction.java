package org.openl.rules.table;

/**
 * Action which resizes merged regions in some grid.
 * 
 * @author PUdalau
 */
public class UndoableResizeRegionAction implements IUndoableGridAction {

    private IGridRegion initialRegion;
    private IGridRegion newRegion;

    public UndoableResizeRegionAction(IGridRegion initialRegion, int numberOfRowsOrColumns, boolean isInsert,
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
    }

    public void doAction(IWritableGrid grid, IUndoGrid undo) {
        grid.removeMergedRegion(initialRegion);
        if (newRegion != null) {
            grid.addMergedRegion(newRegion);
        }
    }

    public void undoAction(IWritableGrid grid, IUndoGrid undo) {
        if (newRegion != null) {
            grid.removeMergedRegion(newRegion);
        }
        grid.addMergedRegion(initialRegion);
    }
}
