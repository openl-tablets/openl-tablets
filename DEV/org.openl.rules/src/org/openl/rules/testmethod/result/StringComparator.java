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
    boolean isEmpty(String object) {
        return object.isEmpty();
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
