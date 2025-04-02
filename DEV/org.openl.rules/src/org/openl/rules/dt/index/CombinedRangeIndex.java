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
     * Constructs a CombinedRangeIndex using the provided range indexes, decision table rule node, and type conversion utility.
     *
     * @param minIndex the index for handling ascending range conditions
     * @param maxIndex the index for handling descending range conditions
     * @param nextNode the subsequent decision table rule node in the composite index
     * @param expressionToParamOpenCast the utility for converting input values to the appropriate condition type
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
     * Retrieves the decision table rule node corresponding to the specified value by performing a two-stage range lookup.
     *
     * <p>If an implicit cast is applicable, converts the input value before evaluation. The method first queries the
     * ascending range index to collect initial matching rules, then refines the results using the descending range index.
     * Providing a non-null static decision triggers an UnsupportedOperationException as static decisions are not supported.</p>
     *
     * @param value the value used for evaluating range conditions
     * @param staticDecision an indicator for static decision-making; must be null since static decisions are not supported
     * @param prevResult the context node used to guide the range lookup process
     * @return a decision table rule node encapsulating the combined results from both range indexes
     * @throws UnsupportedOperationException if staticDecision is non-null
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
