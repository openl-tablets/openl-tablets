package org.openl.rules.dt.index;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.EqualsIndexDecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.algorithm.evaluator.FloatTypeComparator;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.helpers.NumberUtils;

/**
 * A new implementation of Equals Index for decision tables.
 *
 * @author Vladyslav Pikus
 */
public class EqualsIndexV2 extends ARuleIndexV2 {

    static final int[] EMPTY_ARRAY = new int[0];

    private final Map<Object, int[]> index;
    private final ConditionCasts conditionCasts;

    /**
     * Constructs a new EqualsIndexV2 instance.
     *
     * <p>This constructor initializes the index by wrapping the provided map as unmodifiable,
     * passes the next node and empty rule set to the superclass, and validates that the conditionCasts
     * is not null. It also registers all rule indices from the index map into the internal rule set.</p>
     *
     * @param nextNode the decision table rule node used for subsequent evaluations
     * @param index a mapping of condition values to sorted arrays of rule indices
     * @param emptyRules an array of rule indices to use when no matching condition is found
     * @param conditionCasts the instance responsible for casting condition values to the expected types
     */
    public EqualsIndexV2(DecisionTableRuleNode nextNode,
                         Map<Object, int[]> index,
                         int[] emptyRules,
                         ConditionCasts conditionCasts) {
        super(nextNode, emptyRules);
        this.index = Collections.unmodifiableMap(index);
        this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionCasts cannot be null");

        for (var arr : index.values()) {
            for (int ruleN : arr) {
                allRules.set(ruleN);
            }
        }
    }

    /**
     * Returns an array of rule indices associated with the provided value.
     *
     * <p>If the value is non-null, it is first cast to the appropriate condition type and then used
     * to look up corresponding indices in the internal map. If no matching indices are found or if the
     * value is null, an empty array is returned.
     *
     * @param value the value to be used for indexing rules
     * @return an array of rule indices, or an empty array if none are found
     */
    private int[] findIndex(Object value) {
        int[] result = null;
        if (value != null) {
            value = conditionCasts.castToConditionType(value);
            result = index.get(value);
        }
        return result == null ? EMPTY_ARRAY : result;
    }

    /**
     * Computes and returns a new equals index decision table rule node for the given value.
     *
     * <p>This method retrieves the applicable rule indices using {@code findRules(value, prevResult)}
     * and then utilizes the next available index from {@code nextNode} to construct and return
     * an {@code EqualsIndexDecisionTableRuleNode}.</p>
     *
     * @param value the value to match against the index; if null or not found, an empty rule index is used
     * @param prevResult the prior rule node result used for intersecting applicable rules; may be null
     * @return a new {@code EqualsIndexDecisionTableRuleNode} containing the computed rule indices and next index reference
     */
    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new EqualsIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    private int[] findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
            int[] rules = findIndex(value);
            rules = combineSortedArrays(rules, emptyRules);
            return rules;
        }
        return getResultAndIntersect(value, (IDecisionTableRuleNodeV2) prevResult);
    }

    /**
     * Computes the intersection of rule indices between the previous result and the indices associated with the specified value.
     *
     * <p>This method first retrieves the rule indices from the provided previous result. If no previous rules exist, it returns an empty array.
     * Otherwise, it obtains the rules corresponding to the given value, merges them with default empty rules, and returns the intersection of these
     * rules with the previously obtained rule indices.</p>
     *
     * @param value the value used for retrieving rule indices
     * @param prevResult the decision table rule node containing previous rule indices to intersect with
     * @return an array of rule indices present in both the current and previous results, or an empty array if none exist
     */
    private int[] getResultAndIntersect(Object value, IDecisionTableRuleNodeV2 prevResult) {
        int[] prevRes = prevResult.getRules();
        if (prevRes.length == 0) {
            return EMPTY_ARRAY;
        }
        int[] rules = findIndex(value);
        rules = combineSortedArrays(rules, emptyRules);
        return intersectionSortedArrays(rules, prevRes);
    }

    /**
     * Merges two sorted integer arrays into a single sorted array.
     *
     * <p>If one of the arrays is empty, the method returns the non-empty array.
     * The returned array maintains the sorted order, and the merge operation runs in
     * O(a.length + b.length) time.
     *
     * @param a the first sorted array
     * @param b the second sorted array
     * @return a new sorted array containing all elements from both input arrays
     */
    private static int[] combineSortedArrays(int[] a, int[] b) {
        if (a.length == 0) {
            return b;
        }
        if (b.length == 0) {
            return a;
        }
        int[] result = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length) {
            result[k++] = a[i] < b[j] ? a[i++] : b[j++];
        }
        if (i < a.length) {
            System.arraycopy(a, i, result, k, a.length - i);
        }
        if (j < b.length) {
            System.arraycopy(b, j, result, k, b.length - j);
        }
        return result;
    }

    /**
     * Find an intersection between two sorted arrays. Time Complexity: O(a.length + b.length)
     *
     * @param a first array
     * @param b second array
     * @return a new array which contains common elements
     */
    private static int[] intersectionSortedArrays(int[] a, int[] b) {
        int[] result = new int[Math.min(a.length, b.length)];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length) {
            if (a[i] < b[j]) {
                i++;
            } else if (b[j] < a[i]) {
                j++;
            } else {
                result[k++] = b[j++];
                i++;
            }
        }
        if (result.length != k) {
            int[] res = new int[k];
            System.arraycopy(result, 0, res, 0, k);
            return res;
        }
        return result;
    }

    public static class Builder {
        private final DecisionTableRuleNodeBuilder nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        private final DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        private Map<Object, DecisionTableRuleNodeBuilder> map = null;
        private Map<Object, int[]> result = null;
        private boolean comparatorBasedMap = false;

        private ConditionCasts conditionCasts;

        public void addRule(int ruleN) {
            nextNodeBuilder.addRule(ruleN);
        }

        public void putEmptyRule(int ruleN) {
            emptyBuilder.addRule(ruleN);
        }

        public void setConditionCasts(ConditionCasts conditionCasts) {
            this.conditionCasts = conditionCasts;
        }

        public void putValueToRule(Object value, int ruleN) {
            if (comparatorBasedMap && !(value instanceof Comparable<?>)) {
                throw new IllegalArgumentException("Invalid state! Index based on comparable interface.");
            }
            if (map == null) {
                if (NumberUtils.isObjectFloatPointNumber(value)) {
                    if (value instanceof BigDecimal) {
                        map = new TreeMap<>();
                        result = new TreeMap<>();
                    } else {
                        map = new TreeMap<>(FloatTypeComparator.getInstance());
                        result = new TreeMap<>(FloatTypeComparator.getInstance());
                    }
                    comparatorBasedMap = true;
                } else {
                    map = new HashMap<>();
                    result = new HashMap<>();
                }
            }

            DecisionTableRuleNodeBuilder builder = map.computeIfAbsent(value, e -> new DecisionTableRuleNodeBuilder());

            builder.addRule(ruleN);
        }

        public EqualsIndexV2 build() {
            if (map == null) {
                result = Collections.emptyMap();
            } else {
                for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                    result.put(element.getKey(), element.getValue().makeRulesAry());
                }
            }

            return new EqualsIndexV2(nextNodeBuilder.makeNode(), result, emptyBuilder.makeRulesAry(), conditionCasts);
        }
    }

}
