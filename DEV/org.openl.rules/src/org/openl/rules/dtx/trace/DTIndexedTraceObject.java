package org.openl.rules.dtx.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.dtx.IDecisionTableRuleNode;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

public class DTIndexedTraceObject extends DTConditionTraceObject {
    private static final String TRACE_OBJECT_TYPE = "decisionTableIndex";
    private final IDecisionTableRuleNode linkedRule;

    public DTIndexedTraceObject(IDecisionTableTraceObject baseTraceObject, IBaseCondition condition, IDecisionTableRuleNode linkedRule, boolean successful) {
        super(baseTraceObject, condition, -1, successful);
        this.linkedRule = linkedRule;
    }

    @Override
    public String getDisplayName(int mode) {
        int[] rules = linkedRule.getRules();
        IDecisionTable decisionTable = getDecisionTable();

        String[] ruleNames = new String[rules.length];
        for (int i = 0; i < ruleNames.length; i++) {
            ruleNames[i] = decisionTable.getRuleName(rules[i]);
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
            ILogicalTable table = condition.getValueCell(rule);
            regions.addAll(GridTableUtils.getGridRegions(table));
        }

        return regions;
    }

    public IDecisionTableRuleNode getLinkedRule() {
        return linkedRule;
    }
}
