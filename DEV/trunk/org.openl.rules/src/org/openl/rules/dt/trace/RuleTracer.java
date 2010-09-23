package org.openl.rules.dt.trace;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.ICell;
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
        return "Rule: " + decisionTableTraceObject.getDecisionTable().getRuleName(ruleIndex);
    }

    public IGridRegion getGridRegion() {
        return getRuleTable().getGridTable().getRegion();
    }

    public DecisionTableTraceObject getParentTraceObject() {
        return decisionTableTraceObject;
    }

    public ILogicalTable getRuleTable() {
        return decisionTableTraceObject.getDecisionTable().getRuleByIndex(ruleIndex);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return getParentTraceObject().getDecisionTable().getSyntaxNode();
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

    public List<IGridRegion> getGridRegions() {
        
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ILogicalTable ruleTable = getRuleTable();
      
        ICell cell = null;
        for (int row = 0; row < ruleTable.getGridTable().getGridHeight(); row += cell.getHeight()) {
            for (int column = 0; column < ruleTable.getGridTable().getGridWidth(); column += cell.getWidth()) {
                cell = ruleTable.getGridTable().getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
            }
        }
        
        return regions;
    }
}