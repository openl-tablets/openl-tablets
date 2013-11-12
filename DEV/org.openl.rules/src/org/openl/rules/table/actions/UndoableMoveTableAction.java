package org.openl.rules.table.actions;

import org.openl.rules.service.TableService;
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
public class UndoableMoveTableAction extends UndoableEditTableAction {

    private IGridRegion prevRegion = null;
    private IGridRegion newRegion = null;

    public UndoableMoveTableAction() {
    }

    /**
     * @return New region after moving.
     */
    public IGridRegion getNewRegion() {
        return newRegion;
    }

    public void doAction(IGridTable table) {
        IGridTable fullTable = getOriginalTable(table);
        prevRegion = fullTable.getRegion();
        TableService tableService = new TableServiceImpl(false);
        try {
            if (newRegion == null) {
                newRegion = tableService.moveTable(fullTable, null);
            } else {
                tableService.moveTableTo(fullTable, null, newRegion);
            }
        } catch (TableServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void undoAction(IGridTable table) {
        if (newRegion != null) {
            try {
                new TableServiceImpl(false)
                        .moveTableTo(new GridTable(newRegion, table.getGrid()), null, prevRegion);
            } catch (TableServiceException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
