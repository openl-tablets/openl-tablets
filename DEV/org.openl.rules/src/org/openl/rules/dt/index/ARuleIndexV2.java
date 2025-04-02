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
     * Constructs an instance of ARuleIndexV2 with a specified decision table rule node and empty rules.
     * <p>
     * This constructor initializes the ARuleIndexV2 by setting the next rule node, storing the provided empty rules,
     * determining the total number of rules from the given node, and initializing the internal BitSet of all rules.
     * It populates the BitSet using the specified empty rules.
     *
     * @param nextNode the decision table rule node used for subsequent rule resolution
     * @param emptyRules an array of rule numbers considered empty, used to populate the internal rules BitSet
     */
    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
        this.allRules = new BitSet();
        populateAllRules(allRules, emptyRules);
    }

    /**
     * Determines and returns the appropriate decision table rule node based on the provided value, decision mode,
     * and previous node result.
     * <p>
     * If {@code staticDecision} is {@code true}, this method builds a set of applicable rules:
     * <ul>
     *   <li>If {@code prevResult} is not an instance of {@code IDecisionTableRuleNodeV2}, all available rules are applied.</li>
     *   <li>If {@code prevResult} is an instance of {@code IDecisionTableRuleNodeV2} and contains a non-empty rule set,
     *       the method combines that rule set with the available rules by taking their intersection.</li>
     * </ul>
     * It then creates and returns a new {@code RangeIndexDecisionTableRuleNode} using the computed rule set and the index
     * from {@code nextNode}.
     * <p>
     * When {@code staticDecision} is {@code false}, the node lookup is delegated to the overloaded {@code findNode(Object, DecisionTableRuleNode)}
     * method.
     *
     * @param value the value used to determine the relevant rule node
     * @param staticDecision indicates whether a static decision lookup should be performed
     * @param prevResult the previously determined decision table rule node used to limit the rule set, if applicable
     * @return the decision table rule node corresponding to the given parameters
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
 * Determines and returns the decision table rule node for the specified value and previous result.
 *
 * <p>This abstract method is intended for subclasses to implement the logic for selecting the appropriate rule
 * node when static decision processing is not applicable.
 *
 * @param value      the input value used to determine the corresponding rule node
 * @param prevResult the previously computed decision table rule node that serves as a reference for further evaluation
 * @return the decision table rule node corresponding to the provided value and previous result
 */
protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    /**
     * Retrieves an iterable that contains the next decision table rule node.
     *
     * @return an iterable with a single element representing the next decision table rule node.
     */
    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    /**
     * Returns the decision table rule node stub for empty or formula nodes.
     *
     * <p>This method provides a pre-initialized placeholder that serves as the default node
     * when no concrete rule node is applicable, such as in cases of empty or formula-based decisions.</p>
     *
     * @return the empty or formula decision table rule node stub
     */
    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    /**
     * Populates the given BitSet by setting bits for each rule number provided in the rules array.
     *
     * @param allRules the BitSet to update with rule indices
     * @param rules an array of rule numbers whose corresponding bits will be set in allRules
     */
    protected void populateAllRules(BitSet allRules, int[] rules) {
        for (int ruleN : rules) {
            allRules.set(ruleN);
        }
    }

    /**
     * Returns an array of rule indices corresponding to the set bits in the {@code allRules} BitSet.
     *
     * <p>This method iterates through {@code allRules}, collecting the index of each set bit into a new array.
     * The resulting array's length matches the number of set bits in {@code allRules}.</p>
     *
     * @return an array of rule indices representing all set bits in {@code allRules}
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
