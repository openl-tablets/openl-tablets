package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

public class DecisionTableRuleNodeBuilder {

    private List<Integer> rules;

    public DecisionTableRuleNodeBuilder() {
        this.rules = new ArrayList<>();
    }

    public DecisionTableRuleNodeBuilder(DecisionTableRuleNodeBuilder emptyBuilder) {
        this.rules = new ArrayList<>(emptyBuilder.rules);
    }

    public void addRule(int rule) {
        rules.add(Integer.valueOf(rule));
    }

    public DecisionTableRuleNode makeNode() {
        return new DecisionTableRuleNode(makeRulesAry());
    }

    public int[] makeRulesAry() {

        int size = rules.size();
        if (size == 0) {
            return DecisionTableRuleNode.ZERO_ARRAY;
        }
        int[] res = new int[size];

        for (int i = 0; i < res.length; i++) {
            res[i] = rules.get(i);
        }
        return res;
    }
}
