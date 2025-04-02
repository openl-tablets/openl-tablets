package org.openl.rules.dt;

import java.util.BitSet;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.index.BitSetIterator;
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

    /**
     * Determines whether the node has an associated next index.
     *
     * @return true if a next index is present; false otherwise.
     */
    @Override
    public boolean hasIndex() {
        return nextIndex != null;
    }

    /**
     * Returns an iterator over the active rule indices.
     * <p>
     * This iterator efficiently traverses the BitSet without converting it to an array,
     * offering improved performance when iterating over the active rules.
     * </p>
     *
     * @return an iterator over the indices of set bits in the rule set
     */
    @Override
    public IIntIterator getRulesIterator() {
        // use efficient iterator for BitSet to avoid converting to array
        // this is important for performance
        return new BitSetIterator(ruleSet);
    }
}
