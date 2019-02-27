package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;

public interface IRuleIndex {

    DecisionTableRuleNode getEmptyOrFormulaNodes();

    DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    Iterable<? extends DecisionTableRuleNode> nodes();

    int[] collectRules();

}
