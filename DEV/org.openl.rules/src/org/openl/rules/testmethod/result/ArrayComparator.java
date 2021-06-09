package org.openl.rules.testmethod.result;

import java.lang.reflect.Array;

/**
 * @author Yury Molchan
 */
class ArrayComparator extends GenericComparator<Object> {

    private final TestResultComparator elementComparator;

    ArrayComparator(Class<?> clazz, Double delta) {
        this.elementComparator = TestResultComparatorFactory.getComparator(clazz, delta);
    }

    @Override
    boolean fit(Object expected, Object actual) {
        return (expected == null || expected.getClass().isArray()) && (actual == null || actual.getClass().isArray());
    }

    @Override
    boolean isEmpty(Object object) {
        return Array.getLength(object) == 0;
    }

    @Override
    boolean equals(Object expected, Object actual) {
        int len = Array.getLength(actual);
        if (len != Array.getLength(expected)) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            Object actualArrayResult = Array.get(actual, i);
            Object expectedArrayResult = Array.get(expected, i);

            if (!elementComparator.isEqual(expectedArrayResult, actualArrayResult)) {
                return false;
            }
        }

        return true;
    }
}
