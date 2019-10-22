package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.IBaseCondition;
import org.openl.util.CollectionUtils;

public class DTRuleTraceObject extends ATableTracerNode {
    protected final IBaseCondition condition;
    protected boolean successful;
    private String conditionName;
    private final int[] rules;
    private boolean indexed;

    DTRuleTraceObject(IBaseCondition condition, int[] rules, boolean successful, boolean indexed) {
        super("dtRule", null, null, null);
        this.condition = condition;
        this.successful = successful;
        this.conditionName = condition.getName();
        this.rules = rules;
        this.indexed = indexed;
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

    public boolean isIndexed() {
        return indexed;
    }

    public boolean isSuccessful() {
        return successful;
    }

    static DTRuleTraceObject create(Object... args) {
        IBaseCondition condition = (IBaseCondition) args[0];
        int[] rules;
        boolean indexed = false;
        if (args[1] instanceof DecisionTableRuleNode) {
            rules = ((DecisionTableRuleNode) args[1]).getRules();
            indexed = ((DecisionTableRuleNode) args[1]).hasIndex();
        } else {
            rules = new int[] { (Integer) args[1] };
        }
        if (CollectionUtils.isEmpty(rules)) {
            // Don't trace empty rules
            return null;
        }
        boolean arg = (Boolean) args[2];
        return new DTRuleTraceObject(condition, rules, arg, indexed);
    }
}
