package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.method.ExecutableRulesMethod;

public class DecisionTableTraceObject extends ATableTracerNode {

    DecisionTableTraceObject(IDecisionTable decisionTable, Object[] params) {
        super("decisiontable", "DT", (ExecutableRulesMethod)decisionTable, params);
    }

}
