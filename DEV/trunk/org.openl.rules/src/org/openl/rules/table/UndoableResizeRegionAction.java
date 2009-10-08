package org.openl.rules.table;

import org.openl.rules.table.IGridRegion.Tool;

/**
 * Action which changes merged regions in some grid.
 * 
 * @author PUdalau
 */
public class UndoableResizeRegionAction implements IUndoableGridAction {
    private static final boolean RESIZE = false, COPY = true;

    private boolean isInsert;
    private boolean isColumns;
    private int numberOfRowsOrColumns;
    private IGridRegion initialRegion;
    private IGridRegion newRegion;
    private boolean action;

    public UndoableResizeRegionAction(IGridRegion initialRegion, int numberOfRowsOrColumns, boolean isInsert,
            boolean isColumns) {
        this.isInsert = isInsert;
        this.isColumns = isColumns;
        this.numberOfRowsOrColumns = numberOfRowsOrColumns;
        this.initialRegion = initialRegion;
        if (isColumns) {
            if (Tool.width(initialRegion) == numberOfRowsOrColumns) {
                action = COPY;
            } else {
                action = RESIZE;
            }
        } else {
            if (Tool.height(initialRegion) == numberOfRowsOrColumns) {
                action = COPY;
            } else {
                action = RESIZE;
            }
        }
    }

    public void doAction(IWritableGrid grid, IUndoGrid undo) {
        if (action == COPY) {
            copyRegion(grid);
        } else {
            resizeRegion(grid);
        }
    }

    /**
     * Copies initialRegion with shift by numberOfRowsOrColumns. It has sense
     * when we call insertRow on merged cell.
     */
    private void copyRegion(IWritableGrid grid) {
        if (isColumns) {
            newRegion = new GridRegion(initialRegion.getTop(), initialRegion.getLeft() + numberOfRowsOrColumns,
                    initialRegion.getBottom(), initialRegion.getRight() + numberOfRowsOrColumns);
        } else {
            newRegion = new GridRegion(initialRegion.getTop() + numberOfRowsOrColumns, initialRegion.getLeft(),
                    initialRegion.getBottom() + numberOfRowsOrColumns, initialRegion.getRight());
        }
        grid.addMergedRegion(newRegion);

    }

    private void resizeRegion(IWritableGrid grid) {
        grid.removeMergedRegion(initialRegion);
        int increase = isInsert ? numberOfRowsOrColumns : -numberOfRowsOrColumns;
        try {
            if (isColumns) {
                newRegion = new GridRegion(initialRegion.getTop(), initialRegion.getLeft(), initialRegion.getBottom(),
                        initialRegion.getRight() + increase);
            } else {
                newRegion = new GridRegion(initialRegion.getTop(), initialRegion.getLeft(), initialRegion.getBottom()
                        + increase, initialRegion.getRight());
            }
            grid.addMergedRegion(newRegion);
        } catch (IllegalArgumentException e) {
            // Wrong region created
            newRegion = null;
        }
    }

    public void undoAction(IWritableGrid grid, IUndoGrid undo) {
        if (newRegion != null) {
            grid.removeMergedRegion(newRegion);
        }
        if (action == RESIZE) {
            grid.addMergedRegion(initialRegion);
        }
    }
}
