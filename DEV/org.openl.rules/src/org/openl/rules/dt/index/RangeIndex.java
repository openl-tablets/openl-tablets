package org.openl.rules.dt.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.dt.DecisionTableIndexedRuleNode;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeIndex extends ARuleIndex {

    protected List<DecisionTableIndexedRuleNode<?>> index;
    public Comparator<? super Object> comparator = null;

    protected IRangeAdaptor<?, ?> adaptor;

    public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes,
            List<DecisionTableIndexedRuleNode<?>> index,
            IRangeAdaptor<?, ?> adaptor) {
        super(emptyOrFormulaNodes);

        this.index = index;
        this.adaptor = adaptor;
    }

    @Override
    DecisionTableRuleNode findNodeInIndex(Object value) {
        if (index.isEmpty()) {
            // there is no values in index to compare => no reason to search
            return null;
        }

        if (adaptor != null) {
            // Converts value for binary search in index
            // Because different subclasses of Number are not comparable.
            value = adaptor.adaptValueType(value);
        }

        int idx = Collections.binarySearch(index, value, comparator);

        if (idx >= 0) {
            return index.get(idx);
        }

        int insertionPoint = -(idx + 1);

        if (insertionPoint < index.size() && insertionPoint > 0) {
            return index.get(insertionPoint - 1);
        }

        return null;
    }

    @Override
    public Iterable<DecisionTableIndexedRuleNode<?>> nodes() {
        return index;
    }
}
