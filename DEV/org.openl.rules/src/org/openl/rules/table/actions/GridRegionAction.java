package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

public class GridRegionAction implements IUndoableGridTableAction {

    public enum ActionType {
        MOVE,
        EXPAND;
    }

    private IGridRegion region;
    private ActionType actionType;
    private boolean isInsert;
    private boolean isColumns;
    private int nRowsOrColumns;

    public GridRegionAction(IGridRegion region,
            boolean isColumns,
            boolean isInsert,
            ActionType actionType,
            int nRowsOrColumns) {
        this.region = region;
        this.actionType = actionType;
        this.isColumns = isColumns;
        this.isInsert = isInsert;
        this.nRowsOrColumns = nRowsOrColumns;
    }

    @Override
    public void doAction(IGridTable table) {
        switch (actionType) {
            case EXPAND:
                resizeRegion(isInsert, isColumns, nRowsOrColumns, region);
                break;
            case MOVE:
                moveRegion(isInsert, isColumns, nRowsOrColumns, region);
                break;
        }
    }

    @Override
    public void undoAction(IGridTable table) {
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
