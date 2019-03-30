package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class StringComparator extends GenericComparator<String> {

    private static final StringComparator INSTANCE = new StringComparator();

    /**
     * Use {@link #getInstance()} instead.
     */
    private StringComparator() {
    }

    @Override
    boolean fit(Object expected, Object actual) {
        return (expected == null || String.class.equals(expected.getClass())) && (actual == null || String.class
            .equals(actual.getClass()));
    }

    @Override
    boolean isEmpty(String object) {
        return object.isEmpty();
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
