package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class DecisionTableRuleNodeBuilder {

    @Getter
    private final List<Integer> rules;

    public DecisionTableRuleNodeBuilder() {
        this.rules = new ArrayList<>();
    }

    public DecisionTableRuleNodeBuilder(DecisionTableRuleNodeBuilder emptyBuilder) {
        this.rules = new ArrayList<>(emptyBuilder.rules);
    }

    public void addRule(int rule) {
        rules.add(rule);
    }

    public DecisionTableRuleNode makeNode() {
        return new DecisionTableRuleNode(makeRulesAry());
    }

    public int[] makeRulesAry() {

        var size = rules.size();
        if (size == 0) {
            return DecisionTableRuleNode.ZERO_ARRAY;
        }
        int[] res = new int[size];

        for (var i = 0; i < res.length; i++) {
            res[i] = rules.get(i);
        }
        return res;
    }
}
