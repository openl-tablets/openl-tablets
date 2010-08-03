package org.openl.rules.dt.trace;

import java.util.List;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

public class DecisionTableTraceObject extends ATableTracerNode {

    private static final String DECISION_TABLE_TYPE = "decisiontable";

    public DecisionTableTraceObject(DecisionTable decisionTable, Object[] params) {
        super(decisionTable, params);
    }

    public String getDisplayName(int mode) {
        return "DT " + asString(getDecisionTable(), mode);
    }

    public DecisionTable getDecisionTable() {
        return (DecisionTable) getTraceObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return DECISION_TABLE_TYPE;
    }

    @Override
    public String getUri() {
        return getDecisionTable().getSyntaxNode().getUri();
    }

    public RuleTracer traceRule(int i) {
        return new RuleTracer(this, i);
    }

    public List<IGridRegion> getGridRegions() {
        return null;
    }
}
