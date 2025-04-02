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

    /**
     * Constructs a CombinedRangeIndex with the specified range indices, next decision node, and
     * condition type converter.
     *
     * @param minIndex the ascending range index used to retrieve rules based on minimum values
     * @param maxIndex the descending range index used to retrieve rules based on maximum values
     * @param nextNode the next decision table rule node in the evaluation chain
     * @param expressionToParamOpenCast the converter for casting input values to the appropriate condition type
     */
    public CombinedRangeIndex(RangeAscIndex minIndex,
                              RangeDescIndex maxIndex,
                              DecisionTableRuleNode nextNode,
                              IOpenCast expressionToParamOpenCast) {
        this.nextNode = nextNode;
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.castToConditionType = expressionToParamOpenCast;
    }

    /**
     * Finds the decision table rule node corresponding to a given range value.
     *
     * <p>This method first checks whether a static decision is provided. If so, it throws an
     * UnsupportedOperationException since static decisions are not supported. Otherwise, if an implicit
     * conversion is available via a type converter, the input value is converted. Matching rule indices
     * are then determined using a minimum range index, and the result is refined using a maximum range index.
     * The final decision table rule node is constructed using these results and the index of the next node.
     * </p>
     *
     * @param value the input value for matching rules.
     * @param staticDecision must be null; a non-null value triggers an UnsupportedOperationException.
     * @param prevResult the previous decision table rule node used for context in the matching process.
     * @return the decision table rule node corresponding to the matching range criteria.
     * @throws UnsupportedOperationException if staticDecision is not null.
     */
    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (staticDecision != null) {
            throw new UnsupportedOperationException("Static decision is not supported for CombinedRangeIndex");
        }
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
