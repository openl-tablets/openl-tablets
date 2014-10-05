package org.openl.rules.dt.trace;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;

public class DecisionTableTraceObject extends ATableTracerNode {

    public DecisionTableTraceObject(DecisionTable decisionTable, Object[] params) {
        super("decisiontable", "DT", decisionTable, params);
    }

    public DecisionTable getDecisionTable() {
        return (DecisionTable) getTraceObject();
    }
}
