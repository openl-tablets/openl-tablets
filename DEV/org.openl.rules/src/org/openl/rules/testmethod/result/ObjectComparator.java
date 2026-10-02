package org.openl.rules.testmethod.result;

import java.math.BigDecimal;
import java.util.Objects;

import org.openl.rules.convertor.String2DataConvertorFactory;

/**
 * @author Yury Molchan
 */
class ObjectComparator extends GenericComparator<Object> {

    static final TestResultComparator INSTANCE = new ObjectComparator();

    private BigDecimal delta;

    /**
     * Use {@link #INSTANCE} instead.
     */
    private ObjectComparator() {
    }

    ObjectComparator(BigDecimal delta) {
        this.delta = delta;
    }

    @Override
    boolean equals(Object expectedValue, Object actualValue) {
        Class<?> expectedClass = expectedValue.getClass();
        Class<?> actualClass = actualValue.getClass();
        if (expectedClass != actualClass && String.class == expectedClass) {
            try {
                var convertor = String2DataConvertorFactory.getConvertor(actualClass);
                expectedValue = convertor.parse((String) expectedValue, null);
            } catch (Exception ignored) {
                // a text that cannot be converted to the actual type is compared as it is
            }
        }
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(expectedValue.getClass(), delta);
        if (comparator.getClass() != this.getClass()) {
            return comparator.isEqual(expectedValue, actualValue);
        }
        return Objects.equals(expectedValue, actualValue);
    }
}
