package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;

public class GridRegionAction implements IUndoableGridAction {

    public static enum ActionType {
        MOVE,
        EXPAND;
    }

    IGridRegion region;
    ActionType actionType;
    boolean isInsert;
    boolean isColumns;
    int nRowsOrColumns;

    public GridRegionAction(IGridRegion region, boolean isColumns, boolean isInsert, ActionType actionType,
            int nRowsOrColumns) {
        this.region = region;
        this.actionType = actionType;
        this.isColumns = isColumns;
        this.isInsert = isInsert;
        this.nRowsOrColumns = nRowsOrColumns;
    }

    public void doAction(IWritableGrid wgrid) {
        switch (actionType) {
            case EXPAND:
                resizeRegion(isInsert, isColumns, nRowsOrColumns, region);
                break;
            case MOVE:
                moveRegion(isInsert, isColumns, nRowsOrColumns, region);
                break;
        }
    }

    public void undoAction(IWritableGrid wgrid) {
        switch (actionType) {
            case EXPAND:
                resizeRegion(!isInsert, isColumns, nRowsOrColumns, region);
                break;
            case MOVE:
                moveRegion(!isInsert, isColumns, nRowsOrColumns, region);
                break;
        }
    }

    public void resizeRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, IGridRegion r) {
        int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
        if (isColumns) {
            ((GridRegion) r).setRight(r.getRight() + inc);
        } else {
            ((GridRegion) r).setBottom(r.getBottom() + inc);
        }
    }

    public void moveRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, IGridRegion r) {
        int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
        if (isColumns) {
            ((GridRegion) r).setLeft(r.getLeft() + inc);
            ((GridRegion) r).setRight(r.getRight() + inc);
        } else {
            ((GridRegion) r).setTop(r.getTop() + inc);
            ((GridRegion) r).setBottom(r.getBottom() + inc);
        }
    }
}
