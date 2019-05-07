package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.EqualsIndexDecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * A new implementation of Equals Index for decision tables.
 *
 * @author Vladyslav Pikus
 */
public class EqualsIndexV2 implements IRuleIndex {

    private static final int[] EMPTY_ARRAY = new int[0];

    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();

    private Map<Object, int[]> index;
    private int[] emptyRules;
    private DecisionTableRuleNode nextNode;
    private int rulesTotalSize;

    public EqualsIndexV2(DecisionTableRuleNode nextNode,
                         Map<Object, int[]> index,
                         int[] emptyRules) {
        this.index = Collections.unmodifiableMap(index);
        this.emptyRules = emptyRules;
        this.nextNode = nextNode;
        this.rulesTotalSize = nextNode.getRules().length;
    }

    private int[] findIndex(Object value) {
        int[] result = null;
        if (value != null) {
            result = index.get(value);
        }
        return result == null ? EMPTY_ARRAY : result;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
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
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
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
        for(int ruleN : emptyRules) {
            result[k++] = ruleN;
        }
        Arrays.sort(result);
        return result;
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    /**
     * Combine two sorted arrays into one.
     * Time Complexity: O(a.length + b.length)
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
        while (i < a.length) {
            result[k++] = a[i++];
        }
        while (j < b.length) {
            result[k++] = b[j++];
        }
        return result;
    }

    /**
     * Find an intersection between two sorted arrays.
     * Time Complexity: O(a.length + b.length)
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

}
