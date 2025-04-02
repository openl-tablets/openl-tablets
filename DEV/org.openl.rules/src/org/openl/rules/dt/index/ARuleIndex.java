package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ConditionCasts;

/**
 * @author snshor
 */
public abstract class ARuleIndex implements IRuleIndex {

    private final DecisionTableRuleNode emptyOrFormulaNodes;
    private final ConditionCasts conditionCasts;

    ARuleIndex(DecisionTableRuleNode emptyOrFormulaNodes, ConditionCasts conditionCasts) {
        this.emptyOrFormulaNodes = emptyOrFormulaNodes;
        this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionCasts cannot be null");
    }

    /**
     * Returns the decision table rule node representing either empty or formula-based conditions.
     *
     * @return the decision table rule node designated for empty or formula nodes.
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    /**
     * Finds and returns the decision table rule node corresponding to the given value.
     *
     * <p>If a non-null static decision flag is provided, an UnsupportedOperationException is thrown.
     * If the value is null, the default empty or formula node is returned. Otherwise, the value is cast
     * to the appropriate condition type and used to search the index. If no matching node is found, the
     * default node is returned.
     *
     * @param value the value to search for, or null to use the default node
     * @param staticDecision if non-null, triggers an exception as static decisions are not supported
     * @param prevResult a previous result which is ignored in this implementation
     * @return the corresponding decision node from the index or the default empty or formula node if not found
     * @throws UnsupportedOperationException if static decision mode is specified
     */
    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (staticDecision != null) {
            throw new UnsupportedOperationException("Static decision is not supported for EqualsIndex");
        }
        if (value == null) {
            return emptyOrFormulaNodes;
        }

        value = conditionCasts.castToConditionType(value);
        DecisionTableRuleNode node = findNodeInIndex(value);

        return node == null ? emptyOrFormulaNodes : node;
    }

    abstract DecisionTableRuleNode findNodeInIndex(Object value);

    @Override
    public abstract Iterable<? extends DecisionTableRuleNode> nodes();

    @Override
    public int[] collectRules() {
        Set<Integer> set = new HashSet<>();

        for (DecisionTableRuleNode node : nodes()) {
            int[] rules = node.getRules();
            for (int rule : rules) {
                set.add(rule);
            }
        }

        if (emptyOrFormulaNodes != null) {
            int[] rules = emptyOrFormulaNodes.getRules();
            for (int rule : rules) {
                set.add(rule);
            }

        }

        int[] res = new int[set.size()];

        Iterator<Integer> it = set.iterator();

        for (int i = 0; i < res.length && it.hasNext(); i++) {
            res[i] = it.next();
        }

        Arrays.sort(res);

        return res;
    }

}
