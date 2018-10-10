package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.table.ILogicalTable;

/**
 * Tracer leaf for the Decision Table Rule.
 *
 * @author Yury Molchan
 */
public class DTRuleTracerLeaf extends ATableTracerNode {

    private int[] ruleIndexes;

    DTRuleTracerLeaf(int[] ruleIndexes) {
        super("rule", null, null, null);
        this.ruleIndexes = ruleIndexes;
    }

    public String[] getRuleNames() {
        String[] ruleNames = new String[ruleIndexes.length];
        for (int i = 0; i < ruleIndexes.length; i++) {
            ruleNames[i] = ((IDecisionTable) getTraceObject()).getRuleName(ruleIndexes[i]);
        }
        return ruleNames;
    }

    public ILogicalTable[] getRuleTables() {
        ILogicalTable[] logicalTables = new ILogicalTable[ruleIndexes.length];
        for (int i = 0; i < ruleIndexes.length; i++) {
            logicalTables[i] = ((IDecisionTable) getTraceObject()).getRuleTable(ruleIndexes[i]);
        }
        return logicalTables;
    }

    int[] getRuleIndexes() {
        return ruleIndexes;
    }
}
