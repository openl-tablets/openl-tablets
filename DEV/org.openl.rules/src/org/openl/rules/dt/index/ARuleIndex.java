package org.openl.rules.dt.index;

import java.util.*;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ConditionCasts;

/**
 * @author snshor
 *
 */
public abstract class ARuleIndex implements IRuleIndex {

    private DecisionTableRuleNode emptyOrFormulaNodes;
    private ConditionCasts conditionCasts;

    ARuleIndex(DecisionTableRuleNode emptyOrFormulaNodes, ConditionCasts conditionCasts) {
        this.emptyOrFormulaNodes = emptyOrFormulaNodes;
        this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionCasts cannot be null");
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
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
