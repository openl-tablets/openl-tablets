package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Yury Molchan
 */
class CollectionComparator extends GenericComparator<Collection<?>> {

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
            if (!compare(expectedVal, actualVal)) {
                return false;
            }
        }
        return !expectedItr.hasNext() && !actualItr.hasNext();
    }

    private boolean compare(Object expectedVal, Object actualVal) {
        Class<?> clazz;
        if (expectedVal != null) {
            clazz = expectedVal.getClass();
        } else if (actualVal != null) {
            clazz = actualVal.getClass();
        } else {
            return true;
        }
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz);
        return comparator.compareResult(actualVal, expectedVal, 0.00001);
    }
}
