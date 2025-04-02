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
     * Returns the decision table rule node that represents empty or formula nodes.
     *
     * @return the DecisionTableRuleNode used for empty or formula entries.
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    /**
     * Locates the decision table rule node corresponding to the provided value.
     *
     * <p>If a static decision flag is provided (non-null), an UnsupportedOperationException is thrown
     * since static decisions are not supported. If the value is null, the default empty or formula node
     * is returned. Otherwise, the value is cast to its condition-specific type and used to look up a
     * matching node in the index via {@link #findNodeInIndex(Object)}. If no match is found, the default
     * node is returned.</p>
     *
     * @param value the lookup value for finding a matching rule node; may be null to indicate a default lookup
     * @param staticDecision a flag that, if non-null, causes an UnsupportedOperationException since static decisions are not supported
     * @param prevResult a previous lookup result (unused in this implementation)
     * @return the matching DecisionTableRuleNode, or the default node if no suitable match is found or if the value is null
     * @throws UnsupportedOperationException if a static decision is provided
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
