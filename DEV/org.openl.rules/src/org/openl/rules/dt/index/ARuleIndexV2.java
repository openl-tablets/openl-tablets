package org.openl.rules.dt.index;


import java.util.BitSet;
import java.util.Collections;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.EqualsIndexDecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;

public abstract class ARuleIndexV2 implements IRuleIndex {

    static final int[] EMPTY_ARRAY = new int[0];

    protected final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    protected final DecisionTableRuleNode nextNode;
    protected final int[] emptyRules;
    protected final int rulesTotalSize;

    protected ARuleIndexV2(DecisionTableRuleNode nextNode, int[] emptyRules) {
        this.nextNode = nextNode;
        this.emptyRules = emptyRules;
        this.rulesTotalSize = nextNode.getRules().length;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, Boolean staticDecision, DecisionTableRuleNode prevResult) {
        if (Boolean.TRUE.equals(staticDecision)) {
            if (!(prevResult instanceof IDecisionTableRuleNodeV2)) {
                return new EqualsIndexDecisionTableRuleNode(collectRules(), nextNode.getNextIndex());
            } else {
                var prevRes = ((IDecisionTableRuleNodeV2) prevResult).getRuleSet();
                if (prevRes.isEmpty()) {
                    return new EqualsIndexDecisionTableRuleNode(EMPTY_ARRAY, nextNode.getNextIndex());
                } else {
                    var rules = new BitSet();
                    for (int ruleN : collectRules()) {
                        rules.set(ruleN);
                    }
                    rules.and(prevRes);
                    return new RangeIndexDecisionTableRuleNode(rules, nextNode.getNextIndex());
                }
            }
        }
        return findNode(value, prevResult);
    }

    protected abstract DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult);

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

}
