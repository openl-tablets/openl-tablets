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
     * Constructs a CombinedRangeIndex that manages decision table rule nodes using range-based indices.
     *
     * <p>This constructor initializes the ascending (minIndex) and descending (maxIndex) range indices used
     * for rule selection based on minimum and maximum constraints, respectively, and sets up the chaining node
     * for continued decision table evaluation. It also accepts a casting utility to convert input values to the
     * required condition type.</p>
     *
     * @param minIndex the range index for identifying rules based on minimum constraints
     * @param maxIndex the range index for identifying rules based on maximum constraints
     * @param nextNode the subsequent decision table rule node in the evaluation chain
     * @param expressionToParamOpenCast the casting utility to convert expression values to the needed condition type
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
     * Retrieves a decision table rule node by applying both minimum and maximum range indices to the given value.
     *
     * <p>If a static decision is provided (i.e., non-null), this method throws an UnsupportedOperationException.
     * Otherwise, it optionally converts the input value through an implicit type cast (if configured) and uses
     * the minimum index to initially filter the rules. The resulting node is then refined using the maximum index,
     * and the combined result is returned.
     *
     * @param value the value to be evaluated against the range indices
     * @param staticDecision a flag for static decision logic (unsupported; must be null)
     * @param prevResult the previously computed rule node used for incremental evaluation
     * @return a decision table rule node containing the rules that match the combined range criteria
     * @throws UnsupportedOperationException if staticDecision is not null
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
