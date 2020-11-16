package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeAscIndex implements IRuleIndex {

    protected final List<IndexNode> index;
    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    private final DecisionTableRuleNode nextNode;
    private final IRangeAdaptor<IndexNode, ?> adaptor;
    private final int[] emptyRules;
    private final int rulesTotalSize;

    public RangeAscIndex(DecisionTableRuleNode nextNode,
            List<IndexNode> index,
            IRangeAdaptor<IndexNode, ?> adaptor,
            int[] emptyRules) {
        this.index = Collections.unmodifiableList(index);
        this.adaptor = adaptor;
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
    }

    private Pair<Integer, Integer> findIndexRange(Object value) {
        if (value == null || index.isEmpty()) {
            // there is no values in index to compare => no reason to search
            return null;
        }
        // Converts value for binary search in index
        // Because different subclasses of Number are not comparable.
        value = adaptor.adaptValueType(value);
        int idx = Collections.binarySearch(index, (IndexNode) value);
        return retrieveIndexRange(idx);
    }

    protected Pair<Integer, Integer> retrieveIndexRange(int idx) {
        if (idx >= 0) {
            return Pair.of(0, idx + 1);
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint <= index.size() && insertionPoint > 0) {
                return Pair.of(0, insertionPoint);
            }
        }
        return null;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    BitSet findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            Pair<Integer, Integer> range = findIndexRange(value);
            return collectAllRules(range);
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    private BitSet collectAllRules(Pair<Integer, Integer> range) {
        BitSet bits = new BitSet();
        for (int ruleN : emptyRules) {
            bits.set(ruleN);
        }
        if (range != null) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                for (int ruleN : index.get(i).getRules()) {
                    bits.set(ruleN);
                }
            }
        }
        return bits;
    }

    private BitSet getResultAndIntersect(Object value, IDecisionTableRuleNodeV2 prevResult) {
        BitSet prevRes = prevResult.getRuleSet();
        if (prevRes.isEmpty()) {
            return prevRes;
        }
        Pair<Integer, Integer> range = findIndexRange(value);
        BitSet result = new BitSet();
        for (int ruleN : emptyRules) {
            if (prevRes.get(ruleN)) {
                result.set(ruleN);
            }
        }
        if (range != null) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                for (int ruleN : index.get(i).getRules()) {
                    if (prevRes.get(ruleN)) {
                        result.set(ruleN);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    @Override
    public int[] collectRules() {
        int[] result = new int[rulesTotalSize];
        int k = 0;
        for (IndexNode indexNode : index) {
            for (int ruleN : indexNode.getRules()) {
                result[k++] = ruleN;
            }
        }
        for (int ruleN : emptyRules) {
            result[k++] = ruleN;
        }
        Arrays.sort(result);
        return result;
    }
}
