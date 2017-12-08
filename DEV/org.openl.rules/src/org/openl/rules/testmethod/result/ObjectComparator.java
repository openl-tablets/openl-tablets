package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class ObjectComparator extends GenericComparator<Object> {

    private Double delta;

    ObjectComparator() {
    }

    ObjectComparator(Double delta) {
        this.delta = delta;
    }

    @Override
    boolean equals(Object expectedVal, Object actualVal) {
        Class<?> clazz = expectedVal.getClass();
        if (Object.class.equals(clazz)) {
            return expectedVal.equals(actualVal);
        }
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, delta);
        return comparator.compareResult(actualVal, expectedVal);
    }

}
