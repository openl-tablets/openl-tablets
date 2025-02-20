package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeAscIndex extends ARuleIndexV2 {

    protected final List<IndexNode> index;
    private final IRangeAdaptor<IndexNode, ?> adaptor;

    public RangeAscIndex(DecisionTableRuleNode nextNode,
                         List<IndexNode> index,
                         IRangeAdaptor<IndexNode, ?> adaptor,
                         int[] emptyRules) {
        super(nextNode, emptyRules);
        this.index = Collections.unmodifiableList(index);
        this.adaptor = adaptor;

        for (var node : index) {
            for (int ruleN : node.getRules()) {
                allRules.set(ruleN);
            }
        }
    }

    private IndexRange findIndexRange(Object value) {
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

    protected IndexRange retrieveIndexRange(int idx) {
        if (idx >= 0) {
            return new IndexRange(0, idx + 1);
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint <= index.size() && insertionPoint > 0) {
                return new IndexRange(0, insertionPoint);
            }
        }
        return null;
    }

    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    BitSet findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            var range = findIndexRange(value);
            return collectAllRules(range);
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    private BitSet collectAllRules(IndexRange range) {
        BitSet bits = new BitSet();
        for (int ruleN : emptyRules) {
            bits.set(ruleN);
        }
        if (range != null) {
            for (int i = range.min; i < range.max; i++) {
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
        var range = findIndexRange(value);
        BitSet result = new BitSet();
        for (int ruleN : emptyRules) {
            if (prevRes.get(ruleN)) {
                result.set(ruleN);
            }
        }
        if (range != null) {
            for (int i = range.min; i < range.max; i++) {
                for (int ruleN : index.get(i).getRules()) {
                    if (prevRes.get(ruleN)) {
                        result.set(ruleN);
                    }
                }
            }
        }
        return result;
    }

    protected static final class IndexRange {

        public final int min;
        public final int max;

        public IndexRange(int min, int max) {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid range");
            }
            this.min = min;
            this.max = max;
        }
    }

}
