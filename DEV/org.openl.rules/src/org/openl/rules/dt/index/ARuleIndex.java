package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

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

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (staticDecision != null) {
            throw new UnsupportedOperationException("Static decision is not supported for EqualsIndex");
        }
        if (value == null) {
            return emptyOrFormulaNodes;
        }

        value = conditionCasts.castToConditionType(value);
        var node = findNodeInIndex(value);

        return node == null ? emptyOrFormulaNodes : node;
    }

    abstract DecisionTableRuleNode findNodeInIndex(Object value);

    @Override
    public abstract Iterable<? extends DecisionTableRuleNode> nodes();

    @Override
    public int[] collectRules() {
        var set = new HashSet<Integer>();

        for (DecisionTableRuleNode node : nodes()) {
            var rules = node.getRules();
            for (int rule : rules) {
                set.add(rule);
            }
        }

        if (emptyOrFormulaNodes != null) {
            var rules = emptyOrFormulaNodes.getRules();
            for (int rule : rules) {
                set.add(rule);
            }

        }

        int[] res = new int[set.size()];

        Iterator<Integer> it = set.iterator();

        for (var i = 0; i < res.length && it.hasNext(); i++) {
            res[i] = it.next();
        }

        Arrays.sort(res);

        return res;
    }

}
