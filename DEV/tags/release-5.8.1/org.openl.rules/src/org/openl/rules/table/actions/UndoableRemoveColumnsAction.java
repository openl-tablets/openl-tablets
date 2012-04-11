package org.openl.rules.table.actions;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableRemoveColumnsAction extends UndoableRemoveAction {

    private int nCols;
    private int startCol;
    private int row;

    public UndoableRemoveColumnsAction(int nCols, int startCol, int row) {
        this.nCols = nCols;
        this.startCol = startCol;
        this.row = row;
    }
    
    @Override
    protected boolean canPerformAction(IGridRegion gridRegion) {
        return !(startCol < 0 || startCol >= IGridRegion.Tool.width(gridRegion));
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        int cellWidth = getOriginalTable(table).getCell(startCol, row).getWidth();
        int numberToInsert = nCols;
        if (cellWidth > 1) { // merged cell
            numberToInsert += cellWidth - 1;
        }
        return numberToInsert;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove, IGridRegion fullTableRegion, IGridTable table) {
        return IWritableGrid.Tool.removeColumns(numberToRemove, startCol, fullTableRegion, table);
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove) {
        return new GridRegionAction(gridRegion, COLUMNS, REMOVE, ActionType.EXPAND, numberToRemove);
    }

}
