package org.openl.rules.dt;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

public class RuleTracer extends ATableTracerLeaf {

    private int ruleIndex;
    private DecisionTableTraceObject decisionTableTraceObject;

    public RuleTracer(DecisionTableTraceObject decisionTableTraceObject, int ruleIdx) {
        this.ruleIndex = ruleIdx;
        this.decisionTableTraceObject = decisionTableTraceObject;
    }

    public String getDisplayName(int mode) {
        return "Rule: " + decisionTableTraceObject.getDT().getRuleName(ruleIndex);
    }

    public IGridRegion getGridRegion() {
        return getRuleTable().getGridTable().getRegion();
    }

    public DecisionTableTraceObject getParentTraceObject() {
        return decisionTableTraceObject;
    }

    public ILogicalTable getRuleTable() {
        return decisionTableTraceObject.getDT().getRuleTable(ruleIndex);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return getParentTraceObject().getDT().getTableSyntaxNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "rule";
    }

    @Override
    public String getUri() {
        return getRuleTable().getGridTable().getUri();
    }
}