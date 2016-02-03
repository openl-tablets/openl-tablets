package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.table.ATableTracerNode;

public class DTConditionTraceObject extends ATableTracerNode {
    protected final IBaseCondition condition;
    protected boolean successful;
    protected final int ruleIndex;
    private String conditionName;

    public DTConditionTraceObject(IBaseCondition condition,
            int ruleIndex,
            boolean successful) {
        super("decisionTableCondition", "DT", null, null);
        this.condition = condition;
        this.successful = successful;
        this.ruleIndex = ruleIndex;
        this.conditionName = condition.getName();
    }

    public String getConditionName() {
        return conditionName;
    }

    public int getRuleIndex() {
        return ruleIndex;
    }

    public IBaseCondition getCondition() {
        return condition;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
