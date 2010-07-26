package org.openl.rules.dt.index;

import java.util.Arrays;
import java.util.Iterator;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.util.OpenIterator;

public class RangeIndex extends ARuleIndex {

    private Comparable<?>[] index;
    private DecisionTableRuleNode[] rules;

    public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes, Comparable<Object>[] index, DecisionTableRuleNode[] rules) {
        super(emptyOrFormulaNodes);

        this.index = index;
        this.rules = rules;
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {

        int idx = Arrays.binarySearch(index, convertValueForSearch(value));

        if (idx >= 0) {
            return rules[idx];
        }

        idx = -(idx + 1) - 1;

        return idx < 0 ? null : rules[idx];
    }
    
    /**
     * Converts value for binary search in index(Because different subclasses of {@link Number} are not comparable).
     * 
     * @param value Value to convert
     * @return New value that is adapted for binary search.
     */
    private Object convertValueForSearch(Object value) {
        if(index.length < 1){
            return value; // there is no values in index to compare => no reason
                          // to convert
        }
        if (value instanceof Number) {
            if (index[0] instanceof Long) {
                return ((Number) value).longValue();
            }
            if (index[0] instanceof Double)
                return ((Number) value).doubleValue();
        }
        return value;
    }

    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return OpenIterator.fromArray(rules);
    }

}