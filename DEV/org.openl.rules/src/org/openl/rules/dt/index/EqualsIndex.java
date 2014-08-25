package org.openl.rules.dt.index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.rules.dt.DecisionTableRuleNode;

public class EqualsIndex extends ARuleIndex {

    protected Map<Object, DecisionTableRuleNode> valueNodes;

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes, Map<Object, DecisionTableRuleNode> valueNodes) {
        super(emptyOrFormulaNodes);
        if (valueNodes == null){
            this.valueNodes = new HashMap<Object, DecisionTableRuleNode>();
        }else{
            this.valueNodes = valueNodes;
        }
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {
        if (value != null) {
            return valueNodes.get(value);
        }
        return null;
    }
    
    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return valueNodes.values().iterator();
    }
}
