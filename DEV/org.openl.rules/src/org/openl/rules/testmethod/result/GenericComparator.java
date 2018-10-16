package org.openl.rules.testmethod.result;

/**
 * Compares two objects. An empty object also equals to the null.
 *
 * @author Yury Molchan
 */
class GenericComparator<T> implements TestResultComparator {

    private static final GenericComparator INSTANCE = new GenericComparator();

    /**
     * Use {@link #getInstance()} instead.
     */
    GenericComparator() {
    }

    @Override
    public final boolean isEqual(Object expectedResult, Object actualResult) {
        if (actualResult == expectedResult) {
            return true;
        }
        T expected = (T) expectedResult;
        T actual = (T) actualResult;
        boolean expectedIsEmpty = expected == null || isEmpty(expected);
        boolean actualIsEmpty = actual == null || isEmpty(actual);
        if (expectedIsEmpty) {
            return actualIsEmpty;
        } else if (actualIsEmpty) {
            return false;
        }
        return equals(expected, actual);
    }

    boolean isEmpty(T object) {
        return object == null;
    }

    boolean equals(T expected, T actual) {
        return expected.equals(actual);
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
