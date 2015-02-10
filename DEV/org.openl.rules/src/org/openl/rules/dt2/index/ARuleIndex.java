/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt2.index;

import java.util.Iterator;

import org.openl.rules.dt2.DecisionTableRuleNode;

/**
 * @author snshor
 *
 */
public abstract class ARuleIndex {
	
	boolean hasMetaInfo = false;

    public boolean isHasMetaInfo() {
		return hasMetaInfo;
	}

	public void setHasMetaInfo(boolean hasMetaInfo) {
		this.hasMetaInfo = hasMetaInfo;
	}

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

    public abstract Iterator<DecisionTableRuleNode> nodes();

}
