package org.openl.rules.dt.index;


import java.util.BitSet;
import java.util.Collections;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public abstract class ARuleIndexV2 implements IRuleIndex {

    protected final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    protected final DecisionTableRuleNode nextNode;
    protected final int[] emptyRules;
    protected final int rulesTotalSize;
    protected final BitSet allRules;

    /**
     * Constructs an ARuleIndexV2 instance with the specified decision table rule node and empty rules.
     *
     * <p>This constructor initializes the next decision table rule node and sets the array of empty rules.
     * It determines the total number of rules from the next node's rule set and populates an internal BitSet
     * to represent all empty rules.</p>
     *
     * @param nextNode the decision table rule node providing the base rule set
     * @param emptyRules an array of rule identifiers considered empty
     */
    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
        this.allRules = new BitSet();
        populateAllRules(allRules, emptyRules);
    }

    /**
     * Computes and returns a decision table rule node based on the provided value and the static decision flag.
     *
     * <p>If the staticDecision flag is true, the method constructs a BitSet of applicable rules by either taking all rules 
     * or refining the rule set from prevResult (if it is an instance of IDecisionTableRuleNodeV2), and then returns a new 
     * RangeIndexDecisionTableRuleNode using these rules and the next index. If staticDecision is false, it delegates the 
     * node selection to the abstract dynamic decision node finder.
     *
     * @param value the input value used for determining the matching rule node
     * @param staticDecision flag indicating whether a static decision evaluation should be performed
     * @param prevResult the previously computed decision table rule node, whose rule set may be used for refining the selection
     * @return the decision table rule node corresponding to the evaluated rules
     */
    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (Boolean.TRUE.equals(staticDecision)) {
            BitSet rules = new BitSet();
            if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
                rules.or(allRules);
            } else {
                var prevRes = ((IDecisionTableRuleNodeV2) prevResult).getRuleSet();
                if (!prevRes.isEmpty()) {
                    rules.or(prevRes);
                    rules.and(allRules);
                }
            }
            return new RangeIndexDecisionTableRuleNode(rules, nextNode.getNextIndex());
        }
        return findNode(value, prevResult);
    }

    /**
 * Finds and returns a decision table rule node based on the given value and the previous node result.
 *
 * <p>This method must be implemented by subclasses to compute the appropriate node when a static decision
 * is not applicable.
 *
 * @param value the value used to locate the corresponding decision table rule node
 * @param prevResult the previously evaluated decision table rule node that may influence the final result
 * @return the decision table rule node corresponding to the provided value and context
 */
protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    /**
     * Returns an iterable containing the next decision table rule node.
     *
     * <p>This method wraps the next node instance in a singleton list, providing an iterable view 
     * of the single node contained within this index.
     *
     * @return a singleton iterable with the next decision table rule node
     */
    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    /**
     * Returns the decision table rule node stub used for representing an empty or formula-based node.
     *
     * <p>This stub serves as a placeholder when no concrete decision table rule node exists.
     *
     * @return the empty or formula decision table rule node stub
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    /**
     * Populates the given BitSet by setting each bit corresponding to the rule numbers provided in the array.
     *
     * <p>This method iterates over the supplied rule numbers and marks each as active by setting the corresponding bit in the BitSet.
     *
     * @param allRules the BitSet to update with active rule indices
     * @param rules an array of rule numbers whose corresponding bits are to be set in the BitSet
     */
    protected void populateAllRules(BitSet allRules, int[] rules) {
        for (int ruleN : rules) {
            allRules.set(ruleN);
        }
    }

    /**
     * Collects and returns the indices of all active rules.
     * <p>
     * This method iterates over the internal BitSet representing rule states and gathers the indices
     * of all bits that are set. The resulting array contains the rule indices in ascending order,
     * with the array size matching the total count of active rules.
     * </p>
     *
     * @return an array of integers representing the indices of active rules
     */
    @Override
    public int[] collectRules() {
        int[] result = new int[allRules.cardinality()];
        int index = 0;
        for (int i = allRules.nextSetBit(0); i >= 0; i = allRules.nextSetBit(i + 1)) {
            result[index++] = i;
        }
        return result;
    }

}
