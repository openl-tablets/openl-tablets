package org.openl.rules.dtx.trace;

import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

/**
 * Tracer leaf for the Decision Table Rule.
 *
 * @author DLiauchuk
 */
public class DTRuleTracerLeaf extends ATableTracerLeaf {

    private int ruleIndex;
    private IDecisionTableTraceObject decisionTableTraceObject;

    public DTRuleTracerLeaf(IDecisionTableTraceObject decisionTableTraceObject, int ruleIdx) {
        super("rule");
        this.ruleIndex = ruleIdx;
        this.decisionTableTraceObject = decisionTableTraceObject;
    }

    public IGridRegion getGridRegion() {
        return getRuleTable().getSource().getRegion();
    }

    public IDecisionTableTraceObject getParentTraceObject() {
        return decisionTableTraceObject;
    }

    public int getRuleIndex() {
        return ruleIndex;
    }

    public ILogicalTable getRuleTable() {
        return getParentTraceObject().getDecisionTable().getRuleTable(ruleIndex);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return getParentTraceObject().getDecisionTable().getSyntaxNode();
    }

    @Override
    public String getUri() {
        return getRuleTable().getSource().getUri();
    }

    public List<IGridRegion> getGridRegions() {
        ILogicalTable table = getRuleTable();
        return GridTableUtils.getGridRegions(table);
    }

    /**
     * Overriden to return the result of the Decision Table on trace*
     */
    @Override
    public Object getResult() {
        return getParentTraceObject().getResult();
    }
}
