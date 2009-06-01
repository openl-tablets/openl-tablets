package org.openl.rules.table;

/**
 * Action which changes merged regions in some grid.
 * 
 * @author PUdalau
 */
public class UndoableResizeRegionAction implements IUndoableGridAction {
    private boolean isInsert;
    private boolean isColumns;
    private int numberOfRowsOrColumns;
    private IGridRegion regionToResize;
    private IGridRegion resizedRegion;

    public UndoableResizeRegionAction(IGridRegion regionToResize, int numberOfRowsOrColumns, boolean isInsert,
            boolean isColumns) {
        this.isInsert = isInsert;
        this.isColumns = isColumns;
        this.numberOfRowsOrColumns = numberOfRowsOrColumns;
        this.regionToResize = regionToResize;
    }

    public void doAction(IWritableGrid grid, IUndoGrid undo) {
        grid.removeMergedRegion(regionToResize);
        int increase = isInsert ? numberOfRowsOrColumns : -numberOfRowsOrColumns;
        if (isColumns) {
            resizedRegion = new GridRegion(regionToResize.getTop(), regionToResize.getLeft(), regionToResize
                    .getBottom(), regionToResize.getRight() + increase);
        } else {
            resizedRegion = new GridRegion(regionToResize.getTop(), regionToResize.getLeft(), regionToResize
                    .getBottom()
                    + increase, regionToResize.getRight());
        }
        grid.addMergedRegion(resizedRegion);
    }

    public void undoAction(IWritableGrid grid, IUndoGrid undo) {
        grid.removeMergedRegion(resizedRegion);
        grid.addMergedRegion(regionToResize);
    }

}
