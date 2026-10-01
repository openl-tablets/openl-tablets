package org.openl.rules.testmethod.result;

import java.util.Map;

/**
 * @author Yury Molchan
 */
class MapComparator extends GenericComparator<Map<?, ?>> {

    static final TestResultComparator INSTANCE = new MapComparator();

    /**
     * Use {@link #INSTANCE} instead.
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
}
