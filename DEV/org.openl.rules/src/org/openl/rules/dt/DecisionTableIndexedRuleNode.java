package org.openl.rules.dt;

/**
 * @author Yury Molchan
 */
public class DecisionTableIndexedRuleNode<T> extends DecisionTableRuleNode implements Comparable<T> {

    Comparable<T> indexedValue;

    public DecisionTableIndexedRuleNode(int[] rules, Comparable<T> indexedValue) {
        super(rules);
        this.indexedValue = indexedValue;
    }

    @Override
    public int compareTo(T o) {
        return indexedValue.compareTo(o);
    }
}
