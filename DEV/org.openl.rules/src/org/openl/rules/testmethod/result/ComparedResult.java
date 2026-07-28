package org.openl.rules.testmethod.result;

import lombok.RequiredArgsConstructor;

import org.openl.rules.testmethod.TestStatus;

@RequiredArgsConstructor
public class ComparedResult {

    private final String fieldName;
    private final Object expectedValue;
    private final Object actualValue;
    private final TestStatus status;

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
