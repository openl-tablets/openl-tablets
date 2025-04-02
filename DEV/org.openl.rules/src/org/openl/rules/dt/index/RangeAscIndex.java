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
     * <p>This constructor initializes the immutable list of index nodes and the range adaptor.
     * It also populates an internal BitSet with all rule indices extracted from the provided index nodes,
     * and invokes the superclass constructor with the given next node and empty rule indices.
     *
     * @param nextNode    the next decision table rule node used for chained processing
     * @param index       the list of index nodes representing decision table rules; it is converted to an unmodifiable list
     * @param adaptor     the range adaptor used to adapt index node values for comparison
     * @param emptyRules  the array of rule indices representing empty rules
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
     * Finds the index range for the specified value within the sorted index.
     *
     * <p>
     * This method adapts the input value using a configured adaptor to ensure it is comparable to the
     * indexed entries. It then performs a binary search on the index and computes the corresponding range
     * of matching entries via {@link #retrieveIndexRange(int)}. If the provided value is null or the index is empty,
     * the method returns null.
     * </p>
     *
     * @param value the value to locate in the index; may be null
     * @return an {@code IndexRange} representing the boundaries of the found index segment, or null if the
     *         value is null or no index is available
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
     * Computes an index range based on a search result index.
     * 
     * <p>If {@code idx} is non-negative, returns a range spanning from 0 to {@code idx + 1}. If {@code idx} is negative,
     * it is interpreted as an indicator that an exact match was not found, so the insertion point is calculated as
     * {@code -(idx + 1)}. Provided the insertion point is within valid bounds, a range from 0 to the insertion point is returned;
     * otherwise, {@code null} is returned.
     *
     * @param idx the result index from a search operation; a non-negative value indicates an exact match,
     *            while a negative value represents the bitwise complement of the insertion point
     * @return an {@code IndexRange} object specifying the lower and upper bounds of applicable indices,
     *         or {@code null} if no valid range can be determined
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
     * Creates a new decision table rule node that encapsulates the rules applicable for the given value.
     *
     * <p>This method computes the applicable rules based on the provided value and the previous rule node result,
     * then returns a RangeIndexDecisionTableRuleNode initialized with these rules and the next available index.
     *
     * @param value the value used to determine the applicable rules
     * @param prevResult the previously computed decision table rule node, whose rules may be intersected with the current rules
     * @return a new RangeIndexDecisionTableRuleNode containing the determined rules and the subsequent index
     */
    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    /**
     * Determines the applicable rules for the given value.
     *
     * <p>If the previous result is not of type IDecisionTableRuleNodeV2, the method finds the index range
     * corresponding to the value and collects all rules within that range. Otherwise, it intersects the
     * rules from the previous result with those applicable for the value.</p>
     *
     * @param value the value to evaluate for rule applicability
     * @param prevResult the previous decision table rule node used to refine rule selection; if not an instance
     *                   of IDecisionTableRuleNodeV2, the complete set of rules for the computed range is returned
     * @return a BitSet containing the indices of applicable rules
     */
    BitSet findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            var range = findIndexRange(value);
            return collectAllRules(range);
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    /**
     * Aggregates applicable rule identifiers from both default empty rules and a specified index range.
     *
     * <p>This method initializes a BitSet with the default empty rules, then, if a non-null range is provided, 
     * it adds the rule identifiers from the index entries between the given range's minimum (inclusive) and maximum (exclusive) indices.</p>
     *
     * @param range the index range used to collect additional rule identifiers; if null, only the default empty rules are used
     * @return a BitSet containing all collected rule identifiers
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
     * Intersects the previous rule set with the rules matching the specified value.
     *
     * <p>If the previous rule set is empty, it is returned immediately. Otherwise, the method determines
     * the index range corresponding to the provided value and includes in the result any rules from the
     * default rule set and from the index nodes within that range that are also present in the previous rule set.</p>
     *
     * @param value the value used to determine the matching index range
     * @param prevResult the previous decision table rule node whose rule set is intersected with new matching rules
     * @return a BitSet containing the rules from the previous result that are also present in the matching index range,
     *         including any default rules
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
         * Constructs a new IndexRange with the specified lower and upper bounds.
         *
         * <p>The range is valid only if the minimum value is non-negative and the maximum value is
         * not less than the minimum value. Otherwise, an IllegalArgumentException is thrown.
         *
         * @param min the lower bound of the range, must be non-negative
         * @param max the upper bound of the range, must be greater than or equal to min
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
