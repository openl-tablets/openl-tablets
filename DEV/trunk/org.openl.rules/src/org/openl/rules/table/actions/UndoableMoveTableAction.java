package org.openl.rules.table.actions;

import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * Action for moving table to unoccupied place of grid.
 * 
 * @author PUdalau
 */
public class UndoableMoveTableAction implements IUndoableGridTableAction {

    private IGridTable initilalTable;
    private IGridRegion newRegion = null;

    public UndoableMoveTableAction(IGridTable table) {
        this.initilalTable = table;
    }

    /**
     * @return New region after moving.
     */
    public IGridRegion getNewRegion() {
        return newRegion;
    }

    public void doAction(IGridTable table) {
        try {
            if (newRegion == null) {
                newRegion = new TableServiceImpl(false).moveTable(initilalTable, null);
            } else {
                new TableServiceImpl(false).moveTableTo(initilalTable, null, newRegion);
            }
        } catch (TableServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void undoAction(IGridTable table) {
        if (newRegion != null) {
            try {
                new TableServiceImpl(false)
                        .moveTableTo(new GridTable(newRegion, table.getGrid()), null, initilalTable.getRegion());
            } catch (TableServiceException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
