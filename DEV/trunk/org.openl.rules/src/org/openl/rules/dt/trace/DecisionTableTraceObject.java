package org.openl.rules.dt.trace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.vm.trace.ITracerObject;

public class DecisionTableTraceObject extends ATableTracerNode {

    private static final String DECISION_TABLE_TYPE = "decisiontable";
    private List<ITracerObject> traceResults = new ArrayList<ITracerObject>();
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

    public Iterator<ITracerObject> getTraceResults() {
        return traceResults.iterator();
    }

    public Iterator<ITracerObject> getTraceConditions() {
        return traceConditions.iterator();
    }

    public DTRuleTracerLeaf traceRule(int i) {
        DTRuleTracerLeaf result = new DTRuleTracerLeaf(this, i);
        traceResults.add(result);
        return result;
    }

    public List<IGridRegion> getGridRegions() {
        return null;
    }
}
