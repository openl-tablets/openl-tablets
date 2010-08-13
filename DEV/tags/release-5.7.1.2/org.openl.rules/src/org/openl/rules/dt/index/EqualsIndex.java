package org.openl.rules.dt.index;

import java.util.HashMap;
import java.util.Iterator;

import org.openl.rules.dt.DecisionTableRuleNode;

public class EqualsIndex extends ARuleIndex {

    private HashMap<Object, DecisionTableRuleNode> valueNodes = new HashMap<Object, DecisionTableRuleNode>();

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes, HashMap<Object, DecisionTableRuleNode> valueNodes) {
        super(emptyOrFormulaNodes);

        this.valueNodes = valueNodes;
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {

        if (value != null) {
            return (DecisionTableRuleNode) valueNodes.get(value);
        }

        return null;
    }

    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return valueNodes.values().iterator();
    }
}
