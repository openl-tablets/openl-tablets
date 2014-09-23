package org.openl.rules.dt.trace;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;

public class DecisionTableTraceObject extends ATableTracerNode {

    private static final String DECISION_TABLE_TYPE = "decisiontable";

    public DecisionTableTraceObject(DecisionTable decisionTable, Object[] params) {
        super(decisionTable, params);
    }

    public String getDisplayName(int mode) {
        return "DT " + asString(getDecisionTable(), mode);
    }

    public DecisionTable getDecisionTable() {
        return (DecisionTable) getTraceObject();
    }

    public String getType() {
        return DECISION_TABLE_TYPE;
    }
}
