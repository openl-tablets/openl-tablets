package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.ILogicalTable;

/**
 * Tracer leaf for the Decision Table Rule.
 *
 * @author Yury Molchan
 */
public class DTRuleTracerLeaf extends ATableTracerNode {

    private int ruleIndex;

    public DTRuleTracerLeaf(int ruleIdx) {
        super("rule", null, null, null);
        this.ruleIndex = ruleIdx;
    }

    public String getRuleName() {
        return ((IDecisionTable) getTraceObject()).getRuleName(ruleIndex);
    }

    public ILogicalTable getRuleTable() {
        return ((IDecisionTable) getTraceObject()).getRuleTable(ruleIndex);
    }
}
