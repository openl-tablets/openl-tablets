package org.openl.rules.dt.index;

import java.util.Map;

import org.openl.rules.dt.DecisionTableRuleNode;

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
    DecisionTableRuleNode findNodeInIndex(Object value) {
        if (value != null) {
            return valueNodes.get(value);
        }
        return null;
    }

    @Override
    public Iterable<DecisionTableRuleNode> nodes() {
        return valueNodes.values();
    }
}
