package org.openl.rules.dt.trace;

import org.openl.rules.dt.element.ICondition;

public class DTIndexedTraceObject extends DTConditionTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableIndex";

    public DTIndexedTraceObject(DecisionTableTraceObject baseTraceObject, ICondition condition, int rowNum) {
        super(baseTraceObject, condition, rowNum, true);
    }

    @Override
    public String getDisplayName(int mode) {
        return String.format("Rule: %s, Indexed condition: %s", getDecisionTable().getRuleName(ruleIndex), condition.getName());
    }

    @Override
    public String getType() {
        return TRACE_OBJECT_TYPE;
    }
}
