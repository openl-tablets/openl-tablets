package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;

public interface IRuleIndex {

    DecisionTableRuleNode getEmptyOrFormulaNodes();

    DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult);

    Iterable<DecisionTableRuleNode> nodes();

    int[] collectRules();

    /**
     * Checks whether every rule of the index leaves the condition blank. Then every rule matches whatever the value
     * is, so the condition needs no value.
     */
    boolean isBlank();

}
