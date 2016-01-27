package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.dtx.IDecisionTableRuleNode;

public class DTIndexedTraceObject extends DTConditionTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableIndex";
    private final IDecisionTableRuleNode linkedRule;

    public DTIndexedTraceObject(DecisionTableTraceObject baseTraceObject,
            IBaseCondition condition,
            IDecisionTableRuleNode linkedRule,
            boolean successful) {
        super(baseTraceObject, condition, -1, successful);
        this.linkedRule = linkedRule;
    }

    @Override
    public String getType() {
        return TRACE_OBJECT_TYPE;
    }

    public IDecisionTableRuleNode getLinkedRule() {
        return linkedRule;
    }
}
