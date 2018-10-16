package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Yury Molchan
 */
class CollectionComparator extends GenericComparator<Collection<?>> {

    private static final CollectionComparator INSTANCE = new CollectionComparator();

    private TestResultComparator comparator = TestResultComparatorFactory.getComparator(Object.class, null);

    /**
     * Use {@link #getInstance()} instead.
     */
    private CollectionComparator() {
    }

    @Override
    boolean isEmpty(Collection<?> object) {
        return object.isEmpty();
    }

    @Override
    boolean equals(Collection<?> expected, Collection<?> actual) {
        int size = expected.size();
        if (size != actual.size()) {
            return false;
        }
        Iterator<?> expectedItr = expected.iterator();
        Iterator<?> actualItr = actual.iterator();
        while (expectedItr.hasNext() && actualItr.hasNext()) {
            Object expectedVal = expectedItr.next();
            Object actualVal = actualItr.next();
            if (!comparator.isEqual(expectedVal, actualVal)) {
                return false;
            }
        }
        return !expectedItr.hasNext() && !actualItr.hasNext();
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
