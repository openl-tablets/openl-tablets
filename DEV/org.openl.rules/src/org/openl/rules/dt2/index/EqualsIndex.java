package org.openl.rules.dt2.index;

import java.util.Iterator;
import java.util.Map;

import org.openl.rules.dt2.DecisionTableRuleNode;

public class EqualsIndex extends ARuleIndex {

    protected Map<Object, DecisionTableRuleNode> valueNodes;

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes, Map<Object, DecisionTableRuleNode> valueNodes) {
        super(emptyOrFormulaNodes);
        this.valueNodes = valueNodes;
        assert valueNodes != null;
    }

    /**
     * For traceable purposes. See {@link TraceableEqualsIndex}.
     */
    EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes) {
        super(emptyOrFormulaNodes);
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
