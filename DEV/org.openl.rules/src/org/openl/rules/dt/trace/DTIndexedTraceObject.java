package org.openl.rules.dt.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

public class DTIndexedTraceObject extends DTConditionTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableIndex";
    private final DecisionTableRuleNode linkedRule;

    public DTIndexedTraceObject(DecisionTableTraceObject baseTraceObject, ICondition condition, DecisionTableRuleNode linkedRule, boolean successful) {
        super(baseTraceObject, condition, -1, successful);
        this.linkedRule = linkedRule;
    }

    @Override
    public String getDisplayName(int mode) {
        String[] ruleNames = new String[linkedRule.getRules().length];
        for (int i = 0; i < ruleNames.length; i++) {
            ruleNames[i] = getDecisionTable().getRuleName(linkedRule.getRules()[i]);
        }
        
        return String.format("Indexed condition: %s, Rules: %s", condition.getName(), Arrays.toString(ruleNames));
    }

    @Override
    public String getType() {
        return TRACE_OBJECT_TYPE;
    }

    @Override
    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        for (int rule : linkedRule.getRules()) {
            addRegionsForeRule(regions, condition.getValueCell(rule));
        }
        
        return regions;
    }

    public DecisionTableRuleNode getLinkedRule() {
        return linkedRule;
    }

    private void addRegionsForeRule(List<IGridRegion> regions, ILogicalTable ruleTable) {
        ICell cell = null;
        for (int row = 0; row < ruleTable.getSource().getHeight(); row += cell.getHeight()) {
            for (int column = 0; column < ruleTable.getSource().getWidth(); column += cell.getWidth()) {
                cell = ruleTable.getSource().getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
            }
        }
    }
}
