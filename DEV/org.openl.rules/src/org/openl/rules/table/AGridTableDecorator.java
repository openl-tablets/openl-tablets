package org.openl.rules.table;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public abstract class AGridTableDecorator extends AGridTable {

    protected final IGridTable table;

    @Override
    public IGrid getGrid() {
        return table.getGrid();
    }

    @Override
    public void edit() {
        table.edit();
    }

    @Override
    public void stopEditing() {
        table.stopEditing();
    }

    /**
     * @return Original table which includes this delegated table
     */
    public IGridTable getOriginalGridTable() {
        return table;
    }

}
