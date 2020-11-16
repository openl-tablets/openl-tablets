package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.Collections;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public class CombinedRangeIndex implements IRuleIndex {

    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    private final DecisionTableRuleNode nextNode;

    private final RangeAscIndex minIndex;
    private final RangeDescIndex maxIndex;

    private final IOpenCast castToConditionType;

    public CombinedRangeIndex(RangeAscIndex minIndex,
            RangeDescIndex maxIndex,
            DecisionTableRuleNode nextNode,
            IOpenCast expressionToParamOpenCast) {
        this.nextNode = nextNode;
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.castToConditionType = expressionToParamOpenCast;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        if (castToConditionType != null && castToConditionType.isImplicit()) {
            value = castToConditionType.convert(value);
        }
        BitSet minIndexRules = minIndex.findRules(value, prevResult);
        DecisionTableRuleNode minIndexResult = new RangeIndexDecisionTableRuleNode(minIndexRules, null);
        BitSet maxIndexRules = maxIndex.findRules(value, minIndexResult);
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
        // we assume that both indexes have the same list of rules and no need to merge them
        return minIndex.collectRules();
    }
}
