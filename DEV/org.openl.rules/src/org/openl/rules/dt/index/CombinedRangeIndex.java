package org.openl.rules.dt.index;

import java.util.List;

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
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (staticDecision != null) {
            // both indexes hold the same rules, and a rule is empty only when both of its cells are
            return ARuleIndexV2.decidedNode(
                    Boolean.TRUE.equals(staticDecision) ? collectRules() : emptyRules(),
                    prevResult,
                    nextNode.getNextIndex());
        }
        if (castToConditionType != null && castToConditionType.isImplicit()) {
            value = castToConditionType.convert(value);
        }
        var minIndexRules = minIndex.findRules(value, prevResult);
        var minIndexResult = new RangeIndexDecisionTableRuleNode(minIndexRules, null);
        var maxIndexRules = maxIndex.findRules(value, minIndexResult);
        return new RangeIndexDecisionTableRuleNode(maxIndexRules, nextNode.getNextIndex());
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return List.of(nextNode);
    }

    /**
     * Returns the rules that match whatever the inputs are: the condition holds both of its cells empty.
     */
    private int[] emptyRules() {
        return EqualsIndexV2.intersectionSortedArrays(minIndex.emptyRules, maxIndex.emptyRules);
    }

    @Override
    public int[] collectRules() {
        // we assume that both indexes have the same list of rules and no need to merge them
        return minIndex.collectRules();
    }
}
