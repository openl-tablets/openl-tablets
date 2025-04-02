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

    /**
     * Constructs a new RangeAscIndex instance.
     *
     * <p>This constructor initializes the index with an unmodifiable list of index nodes and sets up the adaptor for value conversion.
     * It also populates an internal BitSet of all rule indices by iterating through the provided index nodes.
     *
     * @param nextNode   the next decision table rule node
     * @param index      the list of index nodes used for rule indexing
     * @param adaptor    the adaptor for converting index node values for comparison purposes
     * @param emptyRules an array of rule indices representing empty rules
     */
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

    /**
     * Finds the index range corresponding to the specified value by performing a binary search on the index.
     *
     * <p>If the value is {@code null} or the index is empty, {@code null} is returned. Otherwise, the value is adapted
     * to the type of the index nodes using the range adaptor, then a binary search is performed on the index list.
     * The binary search result is converted to an {@link IndexRange} via {@link #retrieveIndexRange(int)}.
     *
     * @param value the value to search for after adapting it for type compatibility with the index nodes
     * @return an {@link IndexRange} representing the computed range or {@code null} if the value is {@code null} or
     *         the index is empty
     */
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

    /**
     * Retrieves an index range based on the binary search result.
     *
     * <p>
     * If the specified index is non-negative, an exact match was found, and the resulting range spans from 0 
     * up to (and including) the matched index (i.e., 0 to idx+1). For a negative index, the value is interpreted
     * as an insertion point indicator (calculated as -(idx + 1)); if this insertion point is valid—greater than 0 
     * and within the bounds of the index list—the range spans from 0 to the insertion point. Otherwise, {@code null} 
     * is returned.
     * </p>
     *
     * @param idx the index result from a binary search; a non-negative value indicates a match, while a negative value 
     *            encodes the insertion point as -(idx + 1)
     * @return an {@code IndexRange} corresponding to the calculated bounds, or {@code null} if no valid range exists
     */
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

    /**
     * Creates and returns a new RangeIndexDecisionTableRuleNode based on the evaluated rules for the provided value.
     *
     * <p>This method determines the applicable rules for the given value and, if a previous node result exists,
     * intersects its rules with the newly evaluated ones. It then initializes a new node with the computed rule set
     * and the next index in the decision table.
     *
     * @param value the value used to determine applicable rules
     * @param prevResult the previous decision table rule node whose rules may be intersected with the new evaluation
     * @return a new RangeIndexDecisionTableRuleNode encapsulating the computed rules and next node index
     */
    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    /**
     * Returns the set of rule indices that match the specified value.
     *
     * <p>If the provided previous result is not an instance of the enhanced decision table rule node, the method determines the matching range for the value and collects all corresponding rules.
     * Otherwise, it computes the intersection between the previously determined rules and the rules that match the given value.</p>
     *
     * @param value the value used to determine the matching rule range
     * @param prevResult the previous decision table rule node, whose rules will be intersected with the currently matching rules if applicable
     * @return a BitSet representing the indices of applicable rules
     */
    BitSet findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            var range = findIndexRange(value);
            return collectAllRules(range);
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    /**
     * Collects and returns a BitSet of rule numbers.
     *
     * <p>This method initializes a BitSet with the default empty rules.
     * If a non-null index range is provided, it further adds the rule numbers
     * from each index node in the range (from {@code range.min} inclusive to
     * {@code range.max} exclusive).</p>
     *
     * @param range the range of indices to process; if null, only empty rules are included
     * @return a BitSet representing the union of empty rules and the rule numbers from the specified range
     */
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

    /**
     * Computes the intersection of rules from a previous decision result with those applicable to the specified value.
     *
     * <p>
     * This method retrieves the rule set from the provided previous result. If it is empty, it returns the empty set immediately.
     * Otherwise, it determines an index range based on an adapted form of the given value, then starts with default empty rules
     * and adds any rule numbers from the index nodes within the range that are also present in the previous rule set.
     * </p>
     *
     * @param value the value used to determine the applicable index range for rule selection
     * @param prevResult the previous decision table rule node whose rule set is to be intersected
     * @return a BitSet representing the intersection of the previous rule set and the rules applicable to the specified value
     */
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

        /**
         * Constructs an IndexRange with the specified boundaries.
         *
         * <p>This constructor ensures that the minimum value is non-negative and that the maximum value is not less than the minimum.
         *
         * @param min the lower boundary of the range (must be non-negative)
         * @param max the upper boundary of the range (must be greater than or equal to min)
         * @throws IllegalArgumentException if min is negative or max is less than min
         */
        public IndexRange(int min, int max) {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid range");
            }
            this.min = min;
            this.max = max;
        }
    }

}
