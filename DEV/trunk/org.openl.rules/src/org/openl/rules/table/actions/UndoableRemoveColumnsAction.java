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
public class UndoableRemoveColumnsAction extends UndoableEditTableAction {

    private int nCols;
    private int startCol;
    private int row;

    private IUndoableGridTableAction action;

    public UndoableRemoveColumnsAction(int nCols, int startCol, int row) {
        this.nCols = nCols;
        this.startCol = startCol;
        this.row = row;
    }

    public void doAction(IGridTable table) {
        IGridRegion fullTableRegion = getOriginalRegion(table);
        if (startCol < 0 || startCol >= IGridRegion.Tool.width(fullTableRegion)) {
            return;
        }
        int cellWidth = table.getCell(startCol, row).getWidth();
        if (cellWidth > 1) { // merged cell
            nCols += cellWidth - 1;
        }
        
        List<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>();
        IUndoableGridTableAction ua = IWritableGrid.Tool.removeColumns(nCols, startCol, fullTableRegion, table);
        actions.add(ua);
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, COLUMNS, REMOVE, ActionType.EXPAND, nCols);
        actions.add(allTable);
        if (isDecoratorTable(table)) {
            GridRegionAction displayTable = new GridRegionAction(
                    table.getRegion(), COLUMNS, REMOVE, ActionType.EXPAND, nCols);
            actions.add(displayTable);
        }
        action = new UndoableCompositeAction(actions);
        action.doAction(table);
    }

    public void undoAction(IGridTable table) {
        action.undoAction(table);
    }

}
