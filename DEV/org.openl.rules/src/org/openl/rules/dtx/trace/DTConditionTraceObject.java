package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.table.ATableTracerNode;

public class DTConditionTraceObject extends ATableTracerNode {
    private final DecisionTableTraceObject baseTraceObject;
    protected final IBaseCondition condition;
    protected boolean successful;
    protected final int ruleIndex;
    private String conditionName;

    public DTConditionTraceObject(DecisionTableTraceObject baseTraceObject,
            IBaseCondition condition,
            int ruleIndex,
            boolean successful) {
        // Avoid cloning parameters for every instance - instead override the
        // method getParameters()
        super("decisionTableCondition", "DT", baseTraceObject.getTraceObject(), new Object[0]);
        this.baseTraceObject = baseTraceObject;
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

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public Object[] getParameters() {
        return baseTraceObject.getParameters();
    }

    @Override
    public Object getResult() {
        return baseTraceObject.getResult();
    }
}
