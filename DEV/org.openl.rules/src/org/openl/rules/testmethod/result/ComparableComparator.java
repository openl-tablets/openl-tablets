package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class ComparableComparator<T extends Comparable<T>> extends GenericComparator<T> {
    @Override
    boolean equals(T expected, T actual) {
        return expected.compareTo(actual) == 0;
    }
}
