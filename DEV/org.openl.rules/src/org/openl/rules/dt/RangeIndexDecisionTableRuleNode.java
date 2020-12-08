package org.openl.rules.dt;

import java.util.BitSet;

import org.openl.rules.dt.index.IRuleIndex;

public class RangeIndexDecisionTableRuleNode extends DecisionTableRuleNode implements IDecisionTableRuleNodeV2 {

    private final BitSet ruleSet;
    private final IRuleIndex nextIndex;

    public RangeIndexDecisionTableRuleNode(BitSet ruleSet, IRuleIndex nextIndex) {
        super(null);
        this.ruleSet = ruleSet;
        this.nextIndex = nextIndex;
    }

    @Override
    public BitSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public int[] getRules() {
        int[] result = new int[ruleSet.cardinality()];
        int i = 0;

        for (int rule = ruleSet.nextSetBit(0); rule >= 0; rule = ruleSet.nextSetBit(rule + 1)) {
            result[i++] = rule;
            if (rule == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }

        return result;
    }

    @Override
    public IRuleIndex getNextIndex() {
        return nextIndex;
    }

    @Override
    public boolean hasIndex() {
        return nextIndex != null;
    }
}
