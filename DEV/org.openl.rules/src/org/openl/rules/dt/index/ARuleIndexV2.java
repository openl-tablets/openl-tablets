package org.openl.rules.dt.index;


import java.util.BitSet;
import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.EqualsIndexDecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public abstract class ARuleIndexV2 implements IRuleIndex {

    static final int[] EMPTY_ARRAY = new int[0];

    protected final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    protected final DecisionTableRuleNode nextNode;
    protected final int[] emptyRules;
    protected final int rulesTotalSize;
    private volatile int[] allRules;

    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (staticDecision != null) {
            // true means that every rule of the index matches, false leaves only the rules with an empty cell
            return decidedNode(Boolean.TRUE.equals(staticDecision) ? collectRules() : emptyRules,
                    prevResult,
                    nextNode.getNextIndex());
        }
        return findNode(value, prevResult);
    }

    /**
     * Builds the answer for a condition the index did not have to look up, keeping the rules the previous
     * condition has left.
     */
    static DecisionTableRuleNode decidedNode(int[] rules, DecisionTableRuleNode prevResult, IRuleIndex nextIndex) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2 prevResultV2)) {
            return new EqualsIndexDecisionTableRuleNode(rules, nextIndex);
        }
        var prevRules = prevResultV2.getRuleSet();
        if (prevRules.isEmpty()) {
            return new EqualsIndexDecisionTableRuleNode(EMPTY_ARRAY, nextIndex);
        }
        var result = new BitSet();
        for (int ruleN : rules) {
            result.set(ruleN);
        }
        result.and(prevRules);
        return new RangeIndexDecisionTableRuleNode(result, nextIndex);
    }

    protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    /**
     * Returns every rule of the index, in the order of the table.
     *
     * <p>A condition that starts with a static check asks for them on every call, so the answer is kept after the
     * first one. The index does not change after it is built, and the returned array must not be modified.
     */
    @Override
    public final int[] collectRules() {
        var rules = allRules;
        if (rules == null) {
            rules = computeRules();
            allRules = rules;
        }
        return rules;
    }

    protected abstract int[] computeRules();

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return List.of(nextNode);
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

}
