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
public class UndoableInsertColumnsAction extends UndoableEditTableAction {

    private int nCols;
    private int beforeCol;
    private int row;

    private IUndoableGridTableAction action;

    public UndoableInsertColumnsAction(int nCols, int beforeCol, int row) {
        this.nCols = nCols;
        this.beforeCol = beforeCol;
        this.row = row;
    }

    public static boolean canInsertColumns(IGridTable table, int nCols) {
        IGridRegion region = getOriginalRegion(table);
        GridRegion newRegion = new GridRegion(region.getTop() - 1, region.getRight() + 1,
                region.getBottom() + 1, region.getRight() + 1 + nCols);
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
        if (!UndoableInsertColumnsAction.canInsertColumns(table, nCols)) {
            try {
                moveTableAction = moveTable(table);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int cellWidth = getOriginalTable(table).getCell(beforeCol, row).getWidth();
        if (cellWidth > 1) { // merged cell
            nCols += cellWidth - 1;
        }
        IGridRegion fullTableRegion = getOriginalRegion(table);
        List<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>();
        IUndoableGridTableAction ua = IWritableGrid.Tool.insertColumns(nCols, beforeCol, fullTableRegion, table);
        actions.add(ua);
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, COLUMNS, INSERT, ActionType.EXPAND, nCols);
        actions.add(allTable);
        if (isDecoratorTable(table)) {
            GridRegionAction displayTable = new GridRegionAction(table.getRegion(), COLUMNS, INSERT, ActionType.EXPAND, nCols);
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
