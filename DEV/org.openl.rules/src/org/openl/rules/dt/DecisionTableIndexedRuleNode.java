package org.openl.rules.dt;

import org.openl.rules.dt.index.ARuleIndex;

/**
 * @author Yury Molchan
 */
public class DecisionTableIndexedRuleNode<T> extends DecisionTableRuleNode implements Comparable<T> {

    Comparable<T> indexedValue;
    int[] emptyRules;
    int[] rules;

    public DecisionTableIndexedRuleNode(int[] emptyRules, int[] rules, Comparable<T> indexedValue) {
        super(null);
        this.indexedValue = indexedValue;
        this.rules = rules;
        this.emptyRules = emptyRules;
    }

    private static int[] merge(int[] a, int[] b) {
        int[] ret = new int[a.length + b.length];
        int i = a.length - 1, j = b.length - 1, k = ret.length;
        while (k > 0)
            ret[--k] = (j < 0 || (i >= 0 && a[i] >= b[j])) ? a[i--] : b[j--];
        return ret;
    }

    @Override
    public int[] getRules() {
        return merge(emptyRules, rules);
    }

    public void setNextIndex(ARuleIndex nextIndex) {
        if (nextIndex != null) {
            this.rules = null;
            this.emptyRules = null;
        }
        super.setNextIndex(nextIndex);
    }

    @Override
    public int compareTo(T o) {
        return indexedValue.compareTo(o);
    }
}
