/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 * 
 */
public abstract class AGridTableDelegator extends AGridTable {
    protected IGridTable gridTable;

    public AGridTableDelegator(IGridTable gridTable) {
        this.gridTable = gridTable;
    }

    public IGrid getGrid() {
        return gridTable.getGrid();
    }
    
    /**
     * @return Original table which includes this delegated table
     */
    public IGridTable getOriginalGridTable() {
        return gridTable;
    }

}
