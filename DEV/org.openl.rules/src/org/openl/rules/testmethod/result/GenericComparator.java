package org.openl.rules.testmethod.result;

/**
 * Compares two objects. An empty object also equals to the null.
 *
 * @author Yury Molchan
 */
class GenericComparator<T> implements TestResultComparator {

    static final TestResultComparator INSTANCE = new GenericComparator<>();

    /**
     * Use {@link #INSTANCE} instead.
     */
    GenericComparator() {
    }

    // A hook: the specialized comparators override it and check the types of both values.
    @SuppressWarnings("java:S1172")
    boolean fit(Object expected, Object actual) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean isEqual(Object expected, Object actual) {
        if (actual == expected) {
            return true;
        }

        if (!fit(expected, actual)) {
            return false;
        }

        var expectedIsEmpty = expected == null || isEmpty((T) expected);
        var actualIsEmpty = actual == null || isEmpty((T) actual);
        if (expectedIsEmpty) {
            return actualIsEmpty;
        } else if (actualIsEmpty) {
            return false;
        }
        return equals((T) expected, (T) actual);
    }

    boolean isEmpty(T object) {
        return object == null;
    }

    boolean equals(T expected, T actual) {
        return expected.equals(actual);
    }
}
