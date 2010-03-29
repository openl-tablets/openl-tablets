package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;

public class RuleRow implements IDecisionTableConstants {
    
    private int row;
    
    private ILogicalTable decisionTable;

    public RuleRow(int row, ILogicalTable table) {
        this.row = row;
        decisionTable = table;
    }

    public String getRuleName(int col) {
        return getValueCell(col).getGridTable().getCell(0, 0).getStringValue();
    }

    private ILogicalTable getValueCell(int col) {
        return decisionTable.getLogicalRegion(col + DATA_COLUMN, row, 1, 1);
    }

}
