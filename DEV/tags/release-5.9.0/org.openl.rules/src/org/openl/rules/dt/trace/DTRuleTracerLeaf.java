package org.openl.rules.dt.trace;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

/**
 * Tracer leaf for the Decision Table Rule.
 * 
 * @author DLiauchuk
 *
 */
public class DTRuleTracerLeaf extends ATableTracerLeaf {
    
    private static final String NAME = "Rule"; 

    private int ruleIndex;
    private DecisionTableTraceObject decisionTableTraceObject;

    public DTRuleTracerLeaf(DecisionTableTraceObject decisionTableTraceObject, int ruleIdx) {
        this.ruleIndex = ruleIdx;
        this.decisionTableTraceObject = decisionTableTraceObject;
    }

    public String getDisplayName(int mode) {
        return String.format("%s: %s", NAME, getParentTraceObject().getDecisionTable().getRuleName(ruleIndex));
    }

    public IGridRegion getGridRegion() {
        return getRuleTable().getSource().getRegion();
    }

    public DecisionTableTraceObject getParentTraceObject() {
        return decisionTableTraceObject;
    }

    public ILogicalTable getRuleTable() {
        return getParentTraceObject().getDecisionTable().getRuleTable(ruleIndex);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return getParentTraceObject().getDecisionTable().getSyntaxNode();
    }

    public String getType() {
        return NAME.toLowerCase();
    }

    @Override
    public String getUri() {
        return getRuleTable().getSource().getUri();
    }

    public List<IGridRegion> getGridRegions() {
        
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ILogicalTable ruleTable = getRuleTable();
      
        ICell cell = null;
        for (int row = 0; row < ruleTable.getSource().getHeight(); row += cell.getHeight()) {
            for (int column = 0; column < ruleTable.getSource().getWidth(); column += cell.getWidth()) {
                cell = ruleTable.getSource().getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
            }
        }
        
        return regions;
    }
    
    /** Overriden to return the result of the Decision Table on trace**/
    @Override
    public Object getResult() {        
        return getParentTraceObject().getResult();
    }
}