package org.openl.rules.dt.index;

import java.math.BigDecimal;
import java.util.Arrays;
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

    private final Map<Object, int[]> index;
    private final ConditionCasts conditionCasts;

    public EqualsIndexV2(DecisionTableRuleNode nextNode,
                         Map<Object, int[]> index,
                         int[] emptyRules,
                         ConditionCasts conditionCasts) {
        super(nextNode, emptyRules);
        this.index = Collections.unmodifiableMap(index);
        this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionCasts cannot be null");
    }

    private int[] findIndex(Object value) {
        int[] result = null;
        if (value != null) {
            value = conditionCasts.castToConditionType(value);
            result = index.get(value);
        }
        return result == null ? EMPTY_ARRAY : result;
    }

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

    private int[] getResultAndIntersect(Object value, IDecisionTableRuleNodeV2 prevResult) {
        int[] prevRes = prevResult.getRules();
        if (prevRes.length == 0) {
            return EMPTY_ARRAY;
        }
        int[] rules = findIndex(value);
        rules = combineSortedArrays(rules, emptyRules);
        return intersectionSortedArrays(rules, prevRes);
    }

    @Override
    public int[] collectRules() {
        int[] result = new int[rulesTotalSize];
        int k = 0;
        for (int[] arr : index.values()) {
            for (int ruleN : arr) {
                result[k++] = ruleN;
            }
        }
        for (int ruleN : emptyRules) {
            result[k++] = ruleN;
        }
        Arrays.sort(result);
        return result;
    }

    /**
     * Combine two sorted arrays into one. Time Complexity: O(a.length + b.length)
     *
     * @param a first array.
     * @param b second array.
     * @return a new array instance. If first argument is empty, it returns second argument. If second is empty - first.
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
