package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableInsertRowsAction extends UndoableEditTableAction {

    private int nRows;
    private int beforeRow;
    private int col;
    
    private IUndoableGridTableAction action;

    public UndoableInsertRowsAction(int nRows, int beforeRow, int col) {
        this.nRows = nRows;
        this.beforeRow = beforeRow;
        this.col = col;
    }

    public static boolean canInsertRows(IGridTable table, int nRows) {
        IGridRegion region = getOriginalRegion(table);
        GridRegion newRegion = new GridRegion(region.getBottom() + 1, region.getLeft() - 1,
                region.getBottom() + 1 + nRows, region.getRight() + 1);
        IGridTable[] allGridTables = table.getGrid().getTables();
        for (int i = 0; i < allGridTables.length; i++) {
            if (!table.getUri().equals(allGridTables[i].getUri())
                    && IGridRegion.Tool.intersects(newRegion, allGridTables[i].getRegion())) {
                return false;
            }
        }
        return true;
    }

    public void doAction(IGridTable table) {
        IUndoableGridTableAction moveTableAction = null;
        if (!UndoableInsertRowsAction.canInsertRows(table, nRows)) {
            try {
                moveTableAction = moveTable(table);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int cellHeight = table.getCell(beforeRow, col).getHeight();
        if (cellHeight > 1) { // merged cell
            nRows += cellHeight - 1;
        }
        IGridRegion fullTableRegion = getOriginalRegion(table);

        List<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>();
        IUndoableGridTableAction ua = IWritableGrid.Tool.insertRows(nRows, beforeRow, fullTableRegion, table);
        actions.add(ua);
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, INSERT, ActionType.EXPAND, nRows);
        actions.add(allTable);
        if (isDecoratorTable(table)) {
            GridRegionAction displayTable = new GridRegionAction(
                    table.getRegion(), ROWS, INSERT, ActionType.EXPAND, nRows);
            actions.add(displayTable);
        }
        action = new UndoableCompositeAction(actions);
        action.doAction(table);
        if (moveTableAction != null) {
            action = new UndoableCompositeAction(moveTableAction, action);
        }
    }

    public void undoAction(IGridTable table) {
        action.undoAction(table);
    }

}
