package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IBaseCondition;

public class DTIndexedTraceObject extends DTConditionTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableIndex";
    private final int[] linkedRule;

    public DTIndexedTraceObject(IBaseCondition condition,
            int[] linkedRule,
            boolean successful) {
        super(condition, -1, successful);
        this.linkedRule = linkedRule;
    }

    @Override
    public String getType() {
        return TRACE_OBJECT_TYPE;
    }

    public int[] getLinkedRule() {
        return linkedRule;
    }
}
