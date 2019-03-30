package org.openl.rules.dt;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt.index.IRuleIndex;

public class DecisionTableRuleNode implements IDecisionTableRuleNode {

    static final int[] ZERO_ARRAY = new int[0];
    private int[] rules;
    private IRuleIndex nextIndex;

    public DecisionTableRuleNode(int[] rules) {
        this.rules = rules;
    }

    public IRuleIndex getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(IRuleIndex nextIndex) {
        this.nextIndex = nextIndex;
        if (nextIndex != null) {
            rules = null;
            // memory optimization: we do not need rule numbers for current
            // index if we have next index
        }
    }

    public int[] getRules() {
        if (rules == null) {
            if (nextIndex == null) {
                throw new RuntimeException("Internal Error: nextIndex is null when rules is null");
            }
            return nextIndex.collectRules();
        }

        return rules;
    }

    public IIntIterator getRulesIterator() {
        return new IntArrayIterator(getRules());
    }

    public boolean hasIndex() {
        return nextIndex != null;
    }

}
