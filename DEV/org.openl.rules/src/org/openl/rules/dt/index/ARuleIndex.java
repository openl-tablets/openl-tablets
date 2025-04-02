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
     * Returns the decision table rule node representing nodes that are either empty or contain formulas.
     *
     * @return the decision table rule node for empty or formula conditions
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    /**
     * Finds the decision table rule node that corresponds to the given value.
     *
     * <p>If a static decision is indicated (i.e. {@code staticDecision} is non-null), an
     * {@link UnsupportedOperationException} is thrown. If the provided {@code value} is {@code null},
     * the default node representing empty or formula nodes is returned. Otherwise, the value is cast
     * to the appropriate condition type and used to locate a matching node in the index. If no
     * matching node is found, the default empty or formula node is returned.
     *
     * @param value the value used to search for a matching rule node; may be {@code null}
     * @param staticDecision flag indicating a static decision request; if non-null, results in an exception
     * @param prevResult a previous result (currently unused)
     * @return the corresponding decision table rule node, or the default node if {@code value} is {@code null} or no match is found
     * @throws UnsupportedOperationException if {@code staticDecision} is non-null
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
