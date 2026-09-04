package org.openl.rules.dt;

import java.util.BitSet;

import lombok.Getter;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.index.BitSetIterator;
import org.openl.rules.dt.index.IRuleIndex;

public class RangeIndexDecisionTableRuleNode extends DecisionTableRuleNode implements IDecisionTableRuleNodeV2 {

    @Getter
    private final BitSet ruleSet;
    @Getter
    private final IRuleIndex nextIndex;

    public RangeIndexDecisionTableRuleNode(BitSet ruleSet, IRuleIndex nextIndex) {
        super(null);
        this.ruleSet = ruleSet;
        this.nextIndex = nextIndex;
    }

    @Override
    public int[] getRules() {
        int[] result = new int[ruleSet.cardinality()];
        var i = 0;

        for (var rule = ruleSet.nextSetBit(0); rule >= 0; rule = ruleSet.nextSetBit(rule + 1)) {
            result[i++] = rule;
            if (rule == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }

        return result;
    }

    @Override
    public boolean hasIndex() {
        return nextIndex != null;
    }

    @Override
    public IIntIterator getRulesIterator() {
        // use efficient iterator for BitSet to avoid converting to array
        // this is important for performance
        return new BitSetIterator(ruleSet);
    }
}
