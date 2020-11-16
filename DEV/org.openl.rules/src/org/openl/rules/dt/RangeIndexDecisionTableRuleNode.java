package org.openl.rules.dt;

import java.util.BitSet;

import org.openl.domain.BitSetIterator;
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
        BitSetIterator it = new BitSetIterator(ruleSet);
        while (it.hasNext()) {
            result[i++] = it.nextInt();
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
