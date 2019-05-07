package org.openl.rules.dt;

import java.util.Arrays;
import java.util.Set;

import org.openl.rules.dt.index.IRuleIndex;

public class RangeIndexDecisionTableRuleNode extends DecisionTableRuleNode implements IDecisionTableRuleNodeV2 {

    private final Set<Integer> ruleSet;
    private final IRuleIndex nextIndex;

    public RangeIndexDecisionTableRuleNode(Set<Integer> ruleSet, IRuleIndex nextIndex) {
        super(null);
        this.ruleSet = ruleSet;
        this.nextIndex = nextIndex;
    }

    @Override
    public Set<Integer> getRuleSet() {
        return ruleSet;
    }

    @Override
    public int[] getRules() {
        int[] result = new int[ruleSet.size()];
        int i = 0;
        for (Integer it : ruleSet) {
            result[i++] = it;
        }
        Arrays.sort(result);
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
