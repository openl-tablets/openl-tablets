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
     * Constructs an EqualsIndexV2 instance by initializing immutable rule indexing,
     * condition casts, and the complete set of rule indices.
     * <p>
     * The provided index is stored as an unmodifiable map and each rule index from its arrays is
     * added to the internal rule set. The condition casts parameter is validated to be non-null.
     * This constructor also passes the next decision table rule node and empty rules array to the superclass.
     * </p>
     *
     * @param nextNode       the subsequent decision table rule node in the evaluation chain.
     * @param index          a map associating condition values with sorted arrays of rule indices.
     * @param emptyRules     a sorted array of rule indices representing empty or default rules.
     * @param conditionCasts the converter for condition types; must not be null.
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
     * Retrieves the rule indices associated with the given condition value.
     * <p>
     * If the specified value is non-null, it is first cast to the appropriate condition type and then used
     * to fetch the corresponding indices from the internal index map. If the value is null or no indices are found,
     * an empty array is returned.
     * </p>
     *
     * @param value the condition value for which the rule indices are to be found; may be null
     * @return an array of rule indices if found; otherwise, an empty array
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
     * Creates a new decision table rule node for the specified value.
     * 
     * <p>
     * This method computes the applicable rules by intersecting the rules retrieved for the given
     * value with those from the previous result, then returns an {@code EqualsIndexDecisionTableRuleNode}
     * with these rules and the next node index.
     * </p>
     *
     * @param value the condition value used to locate matching rules in the index
     * @param prevResult the previous decision table rule node whose rule set is intersected with the current findings
     * @return a new decision table rule node encapsulating the merged rule set and the next node index
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
     * Intersects the rule indices derived from the given value with the rule indices from the previous result.
     *
     * <p>If the previous result contains no rules, this method returns an empty array immediately. Otherwise, it
     * retrieves the current rule indices for the specified value, merges them with a predefined empty rule set,
     * and returns the intersection between these combined indices and the indices from the previous decision
     * table node.</p>
     *
     * @param value the value used to retrieve the current rule indices
     * @param prevResult the decision table rule node containing the previous set of rule indices
     * @return an array of rule indices representing the intersection of the current and previous rule sets
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
     * Merges two sorted int arrays into a single sorted array.
     * <p>
     * If one of the arrays is empty, this method returns the non-empty array directly.
     * The merge operation runs in O(a.length + b.length) time.
     * </p>
     *
     * @param a the first sorted array
     * @param b the second sorted array
     * @return a sorted array containing all elements from both input arrays; if one array is empty, returns the other array
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
