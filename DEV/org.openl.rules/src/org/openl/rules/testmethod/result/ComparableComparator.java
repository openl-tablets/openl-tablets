package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class ComparableComparator<T extends Comparable<T>> extends GenericComparator<T> {

    @SuppressWarnings("rawtypes")
    private static final ComparableComparator INSTANCE = new ComparableComparator();

    /**
     * Use {@link #getInstance()} instead.
     */
    private ComparableComparator() {
    }
    
    @Override
    boolean fit(Object expected, Object actual) {
        return (expected == null || Comparable.class.isAssignableFrom(expected.getClass())) && (actual == null || Comparable.class.isAssignableFrom(actual.getClass()));
    }

    @Override
    boolean equals(T expected, T actual) {
        return expected.compareTo(actual) == 0;
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
