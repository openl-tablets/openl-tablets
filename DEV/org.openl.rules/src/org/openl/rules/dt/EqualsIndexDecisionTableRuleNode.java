package org.openl.rules.dt;

import java.util.BitSet;

import org.openl.rules.dt.index.IRuleIndex;

public class EqualsIndexDecisionTableRuleNode extends DecisionTableRuleNode implements IDecisionTableRuleNodeV2 {

    private final int[] rules;
    private final IRuleIndex nextIndex;

    public EqualsIndexDecisionTableRuleNode(int[] rules, IRuleIndex nextIndex) {
        super(null);
        this.rules = rules;
        this.nextIndex = nextIndex;
    }

    @Override
    public int[] getRules() {
        return rules;
    }

    @Override
    public IRuleIndex getNextIndex() {
        return nextIndex;
    }

    @Override
    public boolean hasIndex() {
        return nextIndex != null;
    }

    @Override
    public BitSet getRuleSet() {
        BitSet result = new BitSet();
        for (int ruleN : rules) {
            result.set(ruleN);
        }
        return result;
    }
}
