package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.runtime.IRuntimeContext;

public class DecisionTableTraceObject extends ATableTracerNode {

    DecisionTableTraceObject(IDecisionTable decisionTable, Object[] params, IRuntimeContext context) {
        super("decisiontable", "DT", (ExecutableRulesMethod) decisionTable, params, context);
    }

}
