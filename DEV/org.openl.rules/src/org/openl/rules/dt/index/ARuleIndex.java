package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.dt.DecisionTableRuleNode;

/**
 * @author snshor
 *
 */
public abstract class ARuleIndex implements IRuleIndex {

    private DecisionTableRuleNode emptyOrFormulaNodes;
    private IOpenCast expressionToParamOpenCast;

    ARuleIndex(DecisionTableRuleNode emptyOrFormulaNodes, IOpenCast expressionToParamOpenCast) {
        this.emptyOrFormulaNodes = emptyOrFormulaNodes;
        this.expressionToParamOpenCast = expressionToParamOpenCast;
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

        if (expressionToParamOpenCast != null && expressionToParamOpenCast.isImplicit()) {
            value = expressionToParamOpenCast.convert(value);
        }

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
