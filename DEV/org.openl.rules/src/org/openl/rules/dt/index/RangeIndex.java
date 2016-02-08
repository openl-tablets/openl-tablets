package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.dt.DecisionTableIndexedRuleNode;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeIndex extends ARuleIndex {

    private List<DecisionTableIndexedRuleNode<?>> index;
    private DecisionTableIndexedRuleNode<?>[] index2;
    public Comparator<? super Object> comparator = null;

    protected IRangeAdaptor<?, ?> adaptor;

    public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes,
            List<DecisionTableIndexedRuleNode<?>> index,
            IRangeAdaptor<?, ?> adaptor) {
        super(emptyOrFormulaNodes);

        this.index = index;
        // Search in a List is slow on IBM Java 6
        index2 = index.toArray(new DecisionTableIndexedRuleNode[index.size()]);
        this.adaptor = adaptor;
    }

    @Override
    DecisionTableRuleNode findNodeInIndex(Object value) {
        if (index2.length == 0) {
            // there is no values in index to compare => no reason to search
            return null;
        }

        if (adaptor != null) {
            // Converts value for binary search in index
            // Because different subclasses of Number are not comparable.
            value = adaptor.adaptValueType(value);
        }

        int idx = Arrays.binarySearch(index2, value, comparator);

        if (idx >= 0) {
            return index2[idx];
        }

        int insertionPoint = -(idx + 1);

        if (insertionPoint < index2.length && insertionPoint > 0) {
            return index2[insertionPoint - 1];
        }

        return null;
    }

    @Override
    public Iterable<DecisionTableIndexedRuleNode<?>> nodes() {
        return index;
    }
}
