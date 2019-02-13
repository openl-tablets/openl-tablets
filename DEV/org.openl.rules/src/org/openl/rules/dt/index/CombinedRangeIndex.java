package org.openl.rules.dt.index;

import java.util.Collections;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public class CombinedRangeIndex implements IRuleIndex {

    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    private final DecisionTableRuleNode nextNode;

    private final RangeAscIndex minIndex;
    private final RangeDescIndex maxIndex;

    public CombinedRangeIndex(RangeAscIndex minIndex, RangeDescIndex maxIndex, DecisionTableRuleNode nextNode) {
        this.nextNode = nextNode;
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        Set<Integer> minIndexRules = minIndex.findRules(value, prevResult);
        DecisionTableRuleNode minIndexResult = new RangeIndexDecisionTableRuleNode(minIndexRules, null);
        Set<Integer> maxIndexRules = maxIndex.findRules(value, minIndexResult);
        return new RangeIndexDecisionTableRuleNode(maxIndexRules, nextNode.getNextIndex());
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    @Override
    public int[] collectRules() {
        //we assume that both indexes have the same list of rules and no need to merge them
        return minIndex.collectRules();
    }
}
