/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.index;

import java.util.Iterator;

import org.openl.rules.dt.DecisionTableRuleNode;

/**
 * @author snshor
 *
 */
public abstract class ARuleIndex {

    private DecisionTableRuleNode emptyOrFormulaNodes;

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

    public abstract Iterator<DecisionTableRuleNode> nodes();

}
