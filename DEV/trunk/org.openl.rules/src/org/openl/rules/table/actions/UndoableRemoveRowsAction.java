package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableRemoveRowsAction extends UndoableEditTableAction {

    private int nRows;
    private int startRow;
    private int col;
    
    private IUndoableGridTableAction action;

    public UndoableRemoveRowsAction(int nRows, int startRow, int col) {
        this.nRows = nRows;
        this.startRow = startRow;
        this.col = col;
    }

    public void doAction(IGridTable table) {
        IGridRegion fullTableRegion = getOriginalRegion(table);
        if (startRow < 0 || startRow >= IGridRegion.Tool.height(fullTableRegion)) {
            return;
        }
        int cellHeight = table.getCell(col, startRow).getHeight();
        if (cellHeight > 1) { // merged cell
            nRows += cellHeight - 1;
        }

        List<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>();
        IUndoableGridTableAction ua = IWritableGrid.Tool.removeRows(nRows, startRow, fullTableRegion, table);
        actions.add(ua);
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, REMOVE, ActionType.EXPAND, nRows);
        actions.add(allTable);
        if (isDecoratorTable(table)) {
            GridRegionAction displayTable = new GridRegionAction(
                    table.getRegion(), ROWS, REMOVE, ActionType.EXPAND, nRows);
            actions.add(displayTable);
        }
        action = new UndoableCompositeAction(actions);
        action.doAction(table);
    }

    public void undoAction(IGridTable table) {
        action.undoAction(table);
    }

}
