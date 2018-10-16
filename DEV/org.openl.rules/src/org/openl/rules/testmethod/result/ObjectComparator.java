package org.openl.rules.testmethod.result;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;

/**
 * @author Yury Molchan
 */
class ObjectComparator extends GenericComparator<Object> {

    private static final ObjectComparator INSTANCE = new ObjectComparator();

    private Double delta;

    /**
     * Use {@link #getInstance()} instead.
     */
    private ObjectComparator() {
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
        Class<?> clazz2 = actualVal.getClass();
        if (Object.class.equals(clazz2)) {
            return expectedVal.equals(actualVal);
        }
        if (!clazz.equals(clazz2)) {
            if (String.class.equals(clazz)) {
                clazz = clazz2;
                try {
                    IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(clazz2);
                    expectedVal = convertor.parse((String) expectedVal, null);
                } catch (Exception ex) {
                    return false;
                }
            } else {
                return false;
            }
        }
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, delta);
        return comparator.isEqual(expectedVal, actualVal);
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
