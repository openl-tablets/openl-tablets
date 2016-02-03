package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.table.ATableTracerNode;

public class DTRuleTraceObject extends ATableTracerNode {
    protected final IBaseCondition condition;
    protected boolean successful;
    private String conditionName;
    private final int[] rules;

    public DTRuleTraceObject(IBaseCondition condition,
            int[] rules,
            boolean successful) {
        super("dtRule", null, null, null);
        this.condition = condition;
        this.successful = successful;
        this.conditionName = condition.getName();
        this.rules = rules;
    }

    public String getConditionName() {
        return conditionName;
    }

    public int[] getRules() {
        return rules;
    }

    public IBaseCondition getCondition() {
        return condition;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
