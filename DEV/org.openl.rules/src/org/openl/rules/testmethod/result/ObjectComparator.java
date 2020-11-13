package org.openl.rules.testmethod.result;

import java.util.Objects;

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
    boolean equals(Object expectedValue, Object actualValue) {
        Class<?> expectedClass = expectedValue.getClass();
        Class<?> actualClass = actualValue.getClass();
        if (expectedClass != actualClass) {
            if (String.class == expectedClass) {
                try {
                    IString2DataConvertor<?> convertor = String2DataConvertorFactory.getConvertor(actualClass);
                    expectedValue = convertor.parse((String) expectedValue, null);
                } catch (Exception ignored) {
                }
            }
            TestResultComparator comparator = TestResultComparatorFactory.getComparator(expectedValue.getClass(), delta);
            if (comparator.getClass() != this.getClass()) {
                return comparator.isEqual(expectedValue, actualValue);
            }
        }
        return Objects.equals(expectedValue, actualValue);
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
