package org.openl.rules.testmethod.result;

/**
 * Compares two objects. An empty object also equals to the null.
 *
 * @author Yury Molchan
 */
class GenericComparator<T> implements TestResultComparator {

    private static final GenericComparator<Object> INSTANCE = new GenericComparator<>();

    /**
     * Use {@link #getInstance()} instead.
     */
    GenericComparator() {
    }

    boolean fit(Object expected, Object actual){
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
        
        boolean expectedIsEmpty = expected == null || isEmpty((T)expected);
        boolean actualIsEmpty = actual == null || isEmpty((T)actual);
        if (expectedIsEmpty) {
            return actualIsEmpty;
        } else if (actualIsEmpty) {
            return false;
        }
        return equals((T)expected, (T)actual);
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
