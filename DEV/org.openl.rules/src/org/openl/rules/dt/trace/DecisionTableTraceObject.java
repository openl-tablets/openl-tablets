package org.openl.rules.dt.trace;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;

public class DecisionTableTraceObject extends ATableTracerNode {

    public DecisionTableTraceObject(DecisionTable decisionTable, Object[] params) {
        super("decisiontable", decisionTable, params);
    }

    public String getDisplayName(int mode) {
        return "DT " + asString(getDecisionTable(), mode);
    }

    public DecisionTable getDecisionTable() {
        return (DecisionTable) getTraceObject();
    }
}
