package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;

public interface IRuleIndex {

    /**
 * Retrieves the DecisionTableRuleNode that represents either an empty node or a formula node.
 *
 * @return the DecisionTableRuleNode corresponding to an empty or formula node
 */
DecisionTableRuleNode getEmptyOrFormulaNodes();

    /**
 * Finds a DecisionTableRuleNode that matches the specified criteria.
 *
 * <p>This method searches for a node based on the provided value and static decision flag.
 * It can also utilize a previously found node (prevResult) to refine or continue the search.</p>
 *
 * @param value the criteria used to match a node
 * @param staticDecision flag indicating if static decision logic should be applied
 * @param prevResult a previously identified node that may influence the search, or null if none exists
 * @return the matching DecisionTableRuleNode, or null if no node meets the criteria
 */
DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult);

    /**
 * Returns an iterable collection of decision table rule nodes.
 *
 * <p>This method provides access to all nodes held in the index, enabling iteration over each
 * decision table rule node.
 *
 * @return an iterable collection of decision table rule nodes
 */
Iterable<? extends DecisionTableRuleNode> nodes();

    int[] collectRules();

}
