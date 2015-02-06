package org.openl.rules.dt2.trace;

import org.openl.rules.dt2.element.ICondition;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

import java.util.List;

public class DTConditionTraceObject extends DecisionTableTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableCondition";
    private final DecisionTableTraceObject baseTraceObject;
    protected final ICondition condition;
    protected boolean successful;
    protected final int ruleIndex;

    public DTConditionTraceObject(DecisionTableTraceObject baseTraceObject, ICondition condition, int ruleIndex,
                                  boolean successful) {
        // Avoid cloning parameters for every instance - instead override the method getParameters()
        super(baseTraceObject.getDecisionTable(), new Object[0]);
        this.baseTraceObject = baseTraceObject;
        this.condition = condition;
        this.successful = successful;
        this.ruleIndex = ruleIndex;
    }

    @Override
    public String getDisplayName(int mode) {
        return String.format("Rule: %s, Condition: %s", getDecisionTable().getRuleName(ruleIndex), condition.getName());
    }

    @Override
    public String getType() {
        return TRACE_OBJECT_TYPE;
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

    @Override
    public List<IGridRegion> getGridRegions() {
        ILogicalTable table = condition.getValueCell(ruleIndex);
        return GridTableUtils.getGridRegions(table);
    }
}
