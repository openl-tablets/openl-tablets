package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.GridRegion;
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
        return "Rule: " + decisionTableTraceObject.getDT().getRuleName(ruleIndex);
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ILogicalTable ruleTable = getRuleTable();
        for (int row = 0; row < ruleTable.getLogicalHeight(); row++) {
            for (int column = 0; column < ruleTable.getLogicalWidth(); column++) {
                ICell cell = ruleTable.getGridTable().getCell(column, row);
                int absoluteRow = cell.getAbsoluteRow();
                int absoluteColumn = cell.getAbsoluteColumn();
                regions.add(new GridRegion(absoluteRow, absoluteColumn, absoluteRow, absoluteColumn));
            }
        }
        return regions;
    }

    public DecisionTableTraceObject getParentTraceObject() {
        return decisionTableTraceObject;
    }

    public ILogicalTable getRuleTable() {
        return decisionTableTraceObject.getDT().getRuleByIndex(ruleIndex);
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