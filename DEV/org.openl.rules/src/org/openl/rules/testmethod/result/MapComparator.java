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
        return (expected == null || Map.class.isAssignableFrom(expected.getClass())) && (actual == null || Map.class.isAssignableFrom(actual.getClass()));
    }
    
    @Override
    boolean isEmpty(Map<?, ?> object) {
        return object.isEmpty();
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
