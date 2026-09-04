package org.openl.rules.dt;

import java.util.BitSet;

import lombok.Getter;

import org.openl.rules.dt.index.IRuleIndex;

public class EqualsIndexDecisionTableRuleNode extends DecisionTableRuleNode implements IDecisionTableRuleNodeV2 {

    @Getter
    private final int[] rules;
    @Getter
    private final IRuleIndex nextIndex;

    public EqualsIndexDecisionTableRuleNode(int[] rules, IRuleIndex nextIndex) {
        super(null);
        this.rules = rules;
        this.nextIndex = nextIndex;
    }

    @Override
    public boolean hasIndex() {
        return nextIndex != null;
    }

    @Override
    public BitSet getRuleSet() {
        var result = new BitSet();
        for (int ruleN : rules) {
            result.set(ruleN);
        }
        return result;
    }
}
