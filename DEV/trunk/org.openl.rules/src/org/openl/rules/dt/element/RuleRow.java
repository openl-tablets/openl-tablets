package org.openl.rules.dt.element;

import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.table.IGridTable;

public class RuleRow {
    
    private int row;
    private IGridTable table;

    public RuleRow(int row, IGridTable table) {
        this.row = row;
        this.table = table;
    }

    public String getRuleName(int col) {
        return getValueCell(col).getGridTable().getCell(0, 0).getStringValue();
    }

    private IGridTable getValueCell(int col) {
        return table.getRegion(col + IDecisionTableConstants.SERVICE_COLUMNS_NUMBER, row, 1, 1);
    }

}
