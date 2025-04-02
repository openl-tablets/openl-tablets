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
     * Initializes the ARuleIndexV2 instance with the specified decision table rule node and empty rule indices.
     *
     * <p>This constructor sets the reference for the subsequent decision table rule node, stores the provided array of empty rule indices,
     * determines the total number of rules from the given node, and creates and populates a BitSet with these empty rule indices using
     * {@link #populateAllRules(BitSet, int[])}.</p>
     *
     * @param nextNode   the decision table rule node used for further rule processing
     * @param emptyRules the array of indices representing empty rules
     */
    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
        this.allRules = new BitSet();
        populateAllRules(allRules, emptyRules);
    }

    /**
     * Finds and returns a decision table rule node based on the provided value and decision context.
     *
     * <p>If the static decision flag is true, the method creates a new rule set by either using all available rules
     * or, when a previous decision node is provided and contains a non-empty rule set, by intersecting that set with the available rules.
     * It then returns a new RangeIndexDecisionTableRuleNode with the computed rules and the next available index.
     * If the static decision flag is false, it delegates the node determination to the overloaded findNode method using the value and previous result.</p>
     *
     * @param value the decision value to evaluate
     * @param staticDecision flag indicating whether to apply static decision logic
     * @param prevResult a previously computed decision node that may contribute its rule set for intersection
     * @return a decision table rule node configured with the appropriate rule set and index
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
 * Finds a decision table rule node based on the specified value and the previous node.
 * <p>
 * Subclasses should implement this method to provide custom logic for selecting the appropriate node,
 * potentially using the previous node as contextual information.
 *
 * @param value the input value used for determining the matching rule node
 * @param prevResult the previous decision table rule node, providing context for the lookup
 * @return the decision table rule node corresponding to the input criteria
 */
protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    /**
     * Returns an iterable that contains the next decision table rule node.
     *
     * <p>This method wraps the "nextNode" in a singleton list to satisfy the requirement
     * of providing an iterable of decision table rule nodes.
     *
     * @return an iterable with a single decision table rule node
     */
    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    /**
     * Returns the stub decision table rule node that represents an empty or formula-based node.
     *
     * @return the empty or formula decision table rule node stub
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    /**
     * Populates the provided BitSet with the specified rule indices.
     *
     * <p>This method marks each rule index from the given array in the BitSet by setting the corresponding bit,
     * enabling efficient tracking of active rules.</p>
     *
     * @param allRules the BitSet to update with rule indices
     * @param rules an array of rule indices to set in the BitSet
     */
    protected void populateAllRules(BitSet allRules, int[] rules) {
        for (int ruleN : rules) {
            allRules.set(ruleN);
        }
    }

    /**
     * Collects and returns an array of indices corresponding to all rules marked in the {@code allRules} BitSet.
     *
     * <p>This method iterates through {@code allRules}, gathering each set bit's index—which represents a rule—
     * into an array. The returned array has a length equal to the number of rules present.
     *
     * @return an array of rule indices where each index corresponds to a set bit in {@code allRules}
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
