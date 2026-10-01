package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class StringComparator extends GenericComparator<String> {

    static final TestResultComparator INSTANCE = new StringComparator();

    /**
     * Use {@link #INSTANCE} instead.
     */
    private StringComparator() {
    }

    @Override
    boolean fit(Object expected, Object actual) {
        return (expected == null || expected instanceof String) && (actual == null || actual instanceof String);
    }

    @Override
    boolean isEmpty(String object) {
        return object.isEmpty();
    }
}
