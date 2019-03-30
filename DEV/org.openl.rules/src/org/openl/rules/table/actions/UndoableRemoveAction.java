package org.openl.rules.table.actions;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * Default behaviour for remove action.
 * 
 * @author DLiauchuk
 *
 */
public abstract class UndoableRemoveAction extends UndoableEditTableAction {
    
    private IUndoableGridTableAction action;

    public void doAction(IGridTable table) {
        IGridRegion fullTableRegion = getOriginalRegion(table);
        if (!canPerformAction(fullTableRegion)) {
            return;
        }
        int numberToRemove = getNumberToRemove(table);
        
        List<IUndoableGridTableAction> actions = new ArrayList<>();
        IUndoableGridTableAction ua = performAction(numberToRemove, fullTableRegion, table);
        actions.add(ua);
        GridRegionAction allTable = getGridRegionAction(fullTableRegion, numberToRemove);
        actions.add(allTable);
        if (isDecoratorTable(table)) {
            GridRegionAction displayTable = getGridRegionAction(table.getRegion(), numberToRemove); 
            actions.add(displayTable);
        }
        action = new UndoableCompositeAction(actions);
        action.doAction(table);

    }

    public void undoAction(IGridTable table) {
        action.undoAction(table);
    }
    
    /**
     * Checks if action can be performed.
     * 
     * @param gridRegion
     * @return true if action can be performed.
     */
    protected abstract boolean canPerformAction(IGridRegion gridRegion);
    
    /**
     * Get actual number of rows or columns to be removed.
     * It depends whether the cell is merged or not.
     * 
     * @param table
     * @return actual number to be removed.
     */
    protected abstract int getNumberToRemove(IGridTable table);
    
    /**
     * Perform action for removing rows or columns.
     * 
     * @param numberToRemove
     * @param fullTableRegion
     * @param table
     * @return action for removing rows or columns.
     */
    protected abstract IUndoableGridTableAction performAction(int numberToRemove, IGridRegion fullTableRegion, IGridTable table);
    
    protected abstract GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove);

}
