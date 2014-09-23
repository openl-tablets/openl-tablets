package org.openl.rules.dt.trace;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.vm.trace.ITracerObject;

import java.util.ArrayList;
import java.util.List;

public class DecisionTableTraceObject extends ATableTracerNode {

    private static final String DECISION_TABLE_TYPE = "decisiontable";
    private List<ITracerObject> traceConditions = new ArrayList<ITracerObject>();

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

    @Override
    public void addChild(ITracerObject child) {
        super.addChild(child);
        traceConditions.add(child);
    }


    public Iterable<ITracerObject> getTraceConditions() {
        return traceConditions;
    }

    public List<IGridRegion> getGridRegions() {
        return null;
    }
}
