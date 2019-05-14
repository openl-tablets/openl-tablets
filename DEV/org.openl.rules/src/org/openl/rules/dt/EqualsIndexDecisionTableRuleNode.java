package org.openl.rules.dt;

import org.openl.rules.dt.index.IRuleIndex;

import java.util.HashSet;
import java.util.Set;

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
    public Set<Integer> getRuleSet() {
        Set<Integer> result = new HashSet<>();
        for (int ruleN : rules) {
            result.add(ruleN);
        }
        return result;
    }
}
