package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

public class DecisionTableRuleNodeBuilder {

    private List<Integer> rules;

    public DecisionTableRuleNodeBuilder() {
        this.rules = new ArrayList<Integer>();
    }

    public DecisionTableRuleNodeBuilder(DecisionTableRuleNodeBuilder emptyBuilder) {
        this.rules = new ArrayList<Integer>(emptyBuilder.rules);
    }

    public void addRule(int rule) {
        rules.add(Integer.valueOf(rule));
    }

    public DecisionTableRuleNode makeNode() {
        return new DecisionTableRuleNode(makeRulesAry());
    }

    public int[] makeRulesAry() {
        
        int[] res = new int[rules.size()];
        
        for (int i = 0; i < res.length; i++) {
            res[i] = rules.get(i);
        }
        
        return res;
    }

}
