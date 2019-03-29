package org.openl.rules.testmethod.result;

import org.openl.rules.testmethod.TestStatus;

public class ComparedResult {

    private final String fieldName;
    private final Object expectedValue;
    private final Object actualValue;
    private final TestStatus status;

    public ComparedResult(String fieldName, Object expectedValue, Object actualValue, TestStatus status) {
        this.fieldName = fieldName;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.status = status;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public Object getActualValue() {
        return actualValue;
    }

    public TestStatus getStatus() {
        return status;
    }
}
