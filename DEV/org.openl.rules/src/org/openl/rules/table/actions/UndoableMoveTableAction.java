package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
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
    private MetaInfoWriter metaInfoWriter;

    public UndoableMoveTableAction(MetaInfoWriter metaInfoWriter) {
        this.metaInfoWriter = metaInfoWriter;
    }

    /**
     * @return New region after moving.
     */
    public IGridRegion getNewRegion() {
        return newRegion;
    }

    @Override
    public void doAction(IGridTable table) {
        IGridTable fullTable = getOriginalTable(table);
        prevRegion = fullTable.getRegion();
        TableServiceImpl tableService = new TableServiceImpl(metaInfoWriter);
        try {
            if (newRegion == null) {
                newRegion = tableService.moveTable(fullTable);
            } else {
                tableService.moveTableTo(fullTable, newRegion);
            }
        } catch (TableServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        if (newRegion != null) {
            try {
                new TableServiceImpl(metaInfoWriter).moveTableTo(new GridTable(newRegion, table.getGrid()), prevRegion);
            } catch (TableServiceException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
