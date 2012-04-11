package org.openl.rules.dt;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.types.IOpenMethod;

public class DecisionTableTraceObject extends ATableTracerNode {

    public DecisionTableTraceObject(DecisionTable decisionTable, Object[] params) {
        super(decisionTable, params);
    }

    public String getDisplayName(int mode) {
        return "DT " + asString((IOpenMethod) getTraceObject(), mode);
    }

    public DecisionTable getDT() {
        return (DecisionTable) getTraceObject();
    }

    public IGridRegion getGridRegion() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "decisiontable";
    }

    @Override
    public String getUri() {
        return getDT().getTableSyntaxNode().getUri();
    }

    public RuleTracer traceRule(int i) {
        return new RuleTracer(this, i);
    }
}
