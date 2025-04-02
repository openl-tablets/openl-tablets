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
     * <p>This constructor initializes the index as an unmodifiable list, sets the range adaptor,
     * and registers all rule numbers from each index node into an internal BitSet for efficient rule retrieval.
     * It also delegates to the superclass constructor with the provided decision table rule node and empty rules.
     *
     * @param nextNode the decision table rule node that handles subsequent evaluations
     * @param index the ordered list of index nodes representing the rule ranges
     * @param adaptor the adaptor used to extract comparable values from the index nodes
     * @param emptyRules the array of rule identifiers representing initially empty rules
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
     * Finds the index range corresponding to the provided value.
     *
     * <p>This method first checks if the provided value is null or if the index is empty. If either condition 
     * is true, it returns null. Otherwise, it adapts the value to a compatible type for comparison using the 
     * adaptor, performs a binary search on the index list, and returns the index range determined by the search result.
     *
     * @param value the value to be searched within the index range
     * @return the index range for the given value, or null if the value is null or the index is empty
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
     * Computes an applicable index range based on the result of a binary search.
     *
     * <p>If the provided index is non-negative, the returned range spans from 0 (inclusive) to (idx + 1)
     * (exclusive). If the index is negative, the function calculates an insertion point using -(idx + 1) and,
     * if the insertion point is valid (greater than 0 and less than or equal to the size of the index list),
     * returns a range from 0 up to that point. Returns {@code null} if no valid range can be determined.
     *
     * @param idx the index obtained from a binary search; a non-negative value indicates an exact match, while a
     *            negative value indicates the insertion point as -(idx + 1)
     * @return an {@code IndexRange} representing the applicable range of indices, or {@code null} if the insertion point is invalid
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
     * Creates a decision table rule node for the specified value.
     * 
     * <p>This method determines the applicable rules for the given value by invoking
     * {@code findRules(value, prevResult)} and then constructs a new {@code RangeIndexDecisionTableRuleNode}
     * using these rules along with the next index from {@code nextNode}.</p>
     *
     * @param value the input value used to determine applicable rules
     * @param prevResult a previously computed rule node for intersecting with the current rule set,
     *                   or {@code null} if no prior result is available
     * @return a new {@code RangeIndexDecisionTableRuleNode} encapsulating the determined rules and the next node index
     */
    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    /**
     * Determines the applicable rules for the given value by either collecting all rules within a computed index range
     * or intersecting them with rules from a previous decision table result.
     *
     * <p>If the provided previous result is not an instance of {@code IDecisionTableRuleNodeV2}, computes the index range
     * for the adapted value and collects all corresponding rules. Otherwise, it intersects the newly found rules
     * with those in the previous result.
     *
     * @param value the value used to determine the applicable rules
     * @param prevResult a previously computed decision table rule node that may be intersected with new rules
     * @return a BitSet containing the set of applicable rules for the given value
     */
    BitSet findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            var range = findIndexRange(value);
            return collectAllRules(range);
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    /**
     * Collects all rule identifiers from the default empty rules and within the specified index range.
     *
     * <p>This method first adds all rule numbers from the default empty rules. If a non-null range is provided,
     * it then iterates over the index nodes from {@code range.min} (inclusive) to {@code range.max} (exclusive),
     * adding each rule number from those nodes to the returned BitSet.</p>
     *
     * @param range the index range specifying the subset of index nodes to process; if {@code null}, only default rules are included
     * @return a BitSet containing the union of rule numbers from the empty rules and the index nodes in the specified range
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
     * Intersects the previous rule set with the applicable rules for the specified value.
     *
     * <p>This method computes the intersection between the rule set from the provided decision table rule node
     * and the rules determined for the given value. If the previous rule set is empty, it is returned immediately.
     * Otherwise, the method includes rules from the predefined empty rule collection and the index nodes within the
     * range identified by the value, adding only the rules already present in the previous rule set.
     * </p>
     *
     * @param value the input value used to determine the applicable range of rules
     * @param prevResult the previous decision table rule node providing the initial rule set for intersection
     * @return a BitSet containing the rules common to both the previous result and those applicable based on the value
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
         * Constructs a new IndexRange with the specified minimum and maximum bounds.
         *
         * <p>The range is inclusive, where {@code min} defines the lower bound and {@code max} the upper bound.
         * The {@code min} value must be non-negative, and the {@code max} value must be greater than or equal to
         * {@code min}.</p>
         *
         * @param min the inclusive lower bound of the range
         * @param max the inclusive upper bound of the range
         * @throws IllegalArgumentException if {@code min} is negative or {@code max} is less than {@code min}
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
