package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;

public interface IRuleIndex {

    /**
 * Returns a DecisionTableRuleNode representing either an empty node or a formula node.
 */
DecisionTableRuleNode getEmptyOrFormulaNodes();

    /**
 * Locates and returns the DecisionTableRuleNode that matches the specified criteria.
 *
 * @param value the key used to identify the appropriate node
 * @param staticDecision if {@code true}, lookup is performed using static decision logic
 * @param prevResult a previously determined rule node that may inform the search
 * @return the matching DecisionTableRuleNode, or {@code null} if no suitable node is found
 */
DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult);

    /**
 * Returns an iterable collection of decision table rule nodes.
 *
 * <p>This iterable allows traversal of all nodes contained in the rule index, enabling inspection
 * and iteration over each decision table rule node.</p>
 *
 * @return an iterable of {@link DecisionTableRuleNode} instances.
 */
Iterable<? extends DecisionTableRuleNode> nodes();

    int[] collectRules();

}
