package org.openl.rules.testmethod.result;

import java.util.Map;

/**
 * @author Yury Molchan
 */
class MapComparator extends GenericComparator<Map<?, ?>> {

    private static final MapComparator INSTANCE = new MapComparator();

    /**
     * Use {@link #getInstance()} instead.
     */
    private MapComparator() {
    }

    @Override
    boolean fit(Object expected, Object actual) {
        return (expected == null || expected instanceof Map) && (actual == null || actual instanceof Map);
    }

    @Override
    boolean isEmpty(Map<?, ?> object) {
        return object.isEmpty();
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
