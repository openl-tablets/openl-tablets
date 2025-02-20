package org.openl.rules.dt.index;


import java.util.BitSet;
import java.util.Collections;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public abstract class ARuleIndexV2 implements IRuleIndex {

    protected final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    protected final DecisionTableRuleNode nextNode;
    protected final int[] emptyRules;
    protected final int rulesTotalSize;
    protected final BitSet allRules;

    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
        this.allRules = new BitSet();
        populateAllRules(allRules, emptyRules);
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (Boolean.TRUE.equals(staticDecision)) {
            BitSet rules = new BitSet();
            if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
                rules.or(allRules);
            } else {
                var prevRes = ((IDecisionTableRuleNodeV2) prevResult).getRuleSet();
                if (!prevRes.isEmpty()) {
                    rules.or(prevRes);
                    rules.and(allRules);
                }
            }
            return new RangeIndexDecisionTableRuleNode(rules, nextNode.getNextIndex());
        }
        return findNode(value, prevResult);
    }

    protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    protected void populateAllRules(BitSet allRules, int[] rules) {
        for (int ruleN : rules) {
            allRules.set(ruleN);
        }
    }

    @Override
    public int[] collectRules() {
        int[] result = new int[allRules.cardinality()];
        int index = 0;
        for (int i = allRules.nextSetBit(0); i >= 0; i = allRules.nextSetBit(i + 1)) {
            result[index++] = i;
        }
        return result;
    }

}
