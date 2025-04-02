package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;

public interface IRuleIndex {

    /**
 * Returns a decision table rule node representing either an empty node or a formula node.
 *
 * @return the decision table rule node for empty or formula-based conditions
 */
DecisionTableRuleNode getEmptyOrFormulaNodes();

    /**
 * Searches for and returns a decision table rule node that matches the specified criteria.
 *
 * <p>This method locates a rule node using the provided value, while the {@code staticDecision}
 * flag determines if the search should be limited to static decision nodes. The {@code prevResult}
 * parameter can supply context from a previous result to continue the search process.</p>
 *
 * @param value the criteria value to locate the rule node
 * @param staticDecision if {@code true}, limits the search to static decision nodes
 * @param prevResult an existing decision table rule node to provide search context, or {@code null} if none
 * @return the matching decision table rule node, or {@code null} if no suitable node is found
 */
DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult);

    /**
 * Returns an iterable collection of decision table rule nodes.
 *
 * <p>This method provides access to all nodes contained in the rule index, allowing for iteration over
 * each {@link DecisionTableRuleNode} instance or its subclasses.
 *
 * @return an iterable collection of decision table rule nodes
 */
Iterable<? extends DecisionTableRuleNode> nodes();

    int[] collectRules();

}
