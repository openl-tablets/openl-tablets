package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dtx.IBaseCondition;

public class DTRuleTraceObject extends ATableTracerNode {
    protected final IBaseCondition condition;
    protected boolean successful;
    private String conditionName;
    private final int[] rules;

    private DTRuleTraceObject(IBaseCondition condition, int[] rules, boolean successful) {
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

    static DTRuleTraceObject create(Object... args) {
        IBaseCondition condition = (IBaseCondition) args[0];
        int[] rules;
        if (args[1] instanceof DecisionTableRuleNode) {
            rules = ((DecisionTableRuleNode) args[1]).getRules();
        } else {
            rules = new int[] { ((Integer) args[1]) };
        }
        boolean arg = (Boolean) args[2];
        return new DTRuleTraceObject(condition, rules, arg);
    }
}
