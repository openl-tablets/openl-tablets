package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.Iterator;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.util.OpenIterator;

public class RangeIndex extends ARuleIndex {

    private Comparable<Object>[] index;
    private DecisionTableRuleNode[] rules;

    public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes, Comparable<Object>[] index, DecisionTableRuleNode[] rules) {
        super(emptyOrFormulaNodes);

        this.index = index;
        this.rules = rules;
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {

        int idx = Arrays.binarySearch(index, value);

        if (idx >= 0) {
            return rules[idx];
        }

        idx = -(idx + 1) - 1;

        return idx < 0 ? null : rules[idx];
    }

    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return OpenIterator.fromArray(rules);
    }

}