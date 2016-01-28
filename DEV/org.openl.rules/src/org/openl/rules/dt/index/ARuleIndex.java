/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;

/**
 * @author snshor
 *
 */
public abstract class ARuleIndex {

    protected DecisionTableRuleNode emptyOrFormulaNodes;

    public ARuleIndex(DecisionTableRuleNode emptyOrFormulaNodes) {
        this.emptyOrFormulaNodes = emptyOrFormulaNodes;
    }

    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyOrFormulaNodes;
    }

    public DecisionTableRuleNode findNode(Object value) {
        if (value == null) {
            return emptyOrFormulaNodes;
        }

        DecisionTableRuleNode node = findNodeInIndex(value);

        return node == null ? emptyOrFormulaNodes : node;
    }

    public abstract DecisionTableRuleNode findNodeInIndex(Object value);

    public abstract Iterable<DecisionTableRuleNode> nodes();

    public int[] collectRules() {
        Set<Integer> set = new HashSet<Integer>();

        for (DecisionTableRuleNode node : nodes()) {
            int[] rules = node.getRules();
            for (int i = 0; i < rules.length; i++) {
                set.add(rules[i]);
            }
        }

        if (emptyOrFormulaNodes != null) {
            int[] rules = emptyOrFormulaNodes.getRules();
            for (int i = 0; i < rules.length; i++) {
                set.add(rules[i]);
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
