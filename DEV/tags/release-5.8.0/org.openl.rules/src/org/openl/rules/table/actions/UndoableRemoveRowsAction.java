package org.openl.rules.table.actions;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableRemoveRowsAction extends UndoableRemoveAction {

    private int nRows;
    private int startRow;
    private int col;

    public UndoableRemoveRowsAction(int nRows, int startRow, int col) {
        this.nRows = nRows;
        this.startRow = startRow;
        this.col = col;
    }

    @Override
    protected boolean canPerformAction(IGridRegion gridRegion) {        
        return !(startRow < 0 || startRow >= IGridRegion.Tool.height(gridRegion));
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove) {
        return new GridRegionAction(gridRegion, ROWS, REMOVE, ActionType.EXPAND, numberToRemove);
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        int cellHeight = getOriginalTable(table).getCell(col, startRow).getHeight();
        int numberToRemove = nRows;
        if (cellHeight > 1) { // merged cell
            numberToRemove += cellHeight - 1;
        }
        return numberToRemove;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove, IGridRegion fullTableRegion, IGridTable table) {        
        return IWritableGrid.Tool.removeRows(numberToRemove, startRow, fullTableRegion, table);
    }

}
