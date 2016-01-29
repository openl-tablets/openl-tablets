package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.Comparator;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeIndex extends ARuleIndex {

    protected Comparable<?>[] index;
    public Comparator<? super Object> comparator = null;
    public DecisionTableRuleNode[] rules;

    protected IRangeAdaptor<?, ?> adaptor;

    public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes,
            Comparable<?>[] index,
            DecisionTableRuleNode[] rules,
            IRangeAdaptor<?, ?> adaptor) {
        super(emptyOrFormulaNodes);

        this.index = index;
        this.rules = rules;
        this.adaptor = adaptor;
    }

    @Override
    DecisionTableRuleNode findNodeInIndex(Object value) {
        if (index.length < 1) {
            // there is no values in index to compare => no reason to search
            return null;
        }

        if (adaptor != null) {
            // Converts value for binary search in index
            // Because different subclasses of Number are not comparable.
            value = adaptor.adaptValueType(value);
        }

        int idx = Arrays.binarySearch(index, value, comparator);

        if (idx >= 0) {
            return rules[idx];
        }

        int insertionPoint = -(idx + 1);

        if (insertionPoint < index.length && insertionPoint > 0) {
            return rules[insertionPoint - 1];
        }

        return null;
    }

    @Override
    public Iterable<DecisionTableRuleNode> nodes() {
        return Arrays.asList(rules);
    }

}