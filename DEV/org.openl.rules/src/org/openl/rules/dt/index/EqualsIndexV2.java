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
     * Constructs a new EqualsIndexV2 instance with the specified parameters.
     *
     * <p>This constructor wraps the provided index in an unmodifiable map, verifies that the condition
     * casts utility is non-null, and aggregates all applicable rule indices for later evaluations.
     *
     * @param nextNode the decision table rule node to be used for subsequent processing
     * @param index a mapping of condition values to arrays of applicable rule indices
     * @param emptyRules an array of fallback rule indices used when no matching condition value is found
     * @param conditionCasts the utility for casting condition values; must not be null
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
     * Returns an array of rule indices corresponding to the given value after casting it to the appropriate condition type.
     *
     * <p>If the provided value is null or if no indices are associated with the cast value, an empty array is returned.</p>
     *
     * @param value the input condition value to cast and use for index lookup
     * @return an array of rule indices for the cast condition value, or an empty array if none are found
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
     * Returns a decision table rule node based on the given condition value and previous node result.
     *
     * <p>This method creates an {@code EqualsIndexDecisionTableRuleNode} by combining the rules
     * applicable to the provided value (retrieved via {@code findRules(value, prevResult)}) with
     * the subsequent index from the node chain.</p>
     *
     * @param value the condition value used to determine matching rules
     * @param prevResult the previous decision table rule node result used for intersecting rule indices
     * @return a new {@code EqualsIndexDecisionTableRuleNode} encapsulating the matching rule indices and next index
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
     * Computes the intersection between the rule indices for a given value and the previously stored rules.
     * 
     * <p>
     * Retrieves the rule indices associated with the specified value, merges them with the default empty rules,
     * and intersects this combined array with the rules obtained from the previous result. If the previous result
     * contains no rules, an empty array is returned.
     * </p>
     *
     * @param value the lookup key used to retrieve rule indices after type casting
     * @param prevResult the decision table rule node containing the set of rules to intersect with
     * @return a sorted array of rule indices present in both the current lookup (after merging with empty rules)
     *         and the previous result, or an empty array if no intersection exists
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
     * <p>This method assumes that both input arrays are sorted in ascending order.
     * It iterates through the arrays, copying the smallest remaining elements into
     * a new array. If one of the arrays is empty, the non-empty array is returned directly.
     *
     * <p>Time complexity: O(a.length + b.length).
     *
     * @param a the first sorted array
     * @param b the second sorted array
     * @return a sorted array containing all elements from both arrays, or the non-empty array if the other is empty
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
