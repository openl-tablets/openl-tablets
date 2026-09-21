package org.openl.rules.table.actions;

import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * Action for moving table to unoccupied place of grid.
 *
 * @author PUdalau
 */
@RequiredArgsConstructor
public class UndoableMoveTableAction extends UndoableEditTableAction {

    private IGridRegion newRegion;
    private final MetaInfoWriter metaInfoWriter;

    /**
     * @return New region after moving.
     */
    public IGridRegion getNewRegion() {
        return newRegion;
    }

    @Override
    public void doAction(IGridTable table) {
        IGridTable fullTable = getOriginalTable(table);
        var tableService = new TableServiceImpl(metaInfoWriter);
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

}
