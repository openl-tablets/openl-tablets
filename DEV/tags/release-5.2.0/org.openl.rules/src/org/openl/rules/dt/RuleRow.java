package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;

public class RuleRow implements IDecisionTableConstants {
    int row;
    ILogicalTable decisionTable;

    public RuleRow(int row, ILogicalTable table) {
        this.row = row;
        decisionTable = table;
    }

    public String getRuleName(int col) {
        return getValueCell(col).getGridTable().getStringValue(0, 0);
    }

    ILogicalTable getValueCell(int col) {
        return decisionTable.getLogicalRegion(col + DATA_COLUMN, row, 1, 1);
    }

}
