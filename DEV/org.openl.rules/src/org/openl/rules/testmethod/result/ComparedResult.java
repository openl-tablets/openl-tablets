package org.openl.rules.testmethod.result;

import org.openl.rules.testmethod.TestStatus;

public class ComparedResult {
    private String fieldName;
    private Object expectedValue;
    private Object actualValue;
    private TestStatus status;
    
    public ComparedResult() {        
    }
    
    public String getFieldName() {
        return fieldName;
    }
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    public Object getExpectedValue() {
        return expectedValue;
    }
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    public Object getActualValue() {
        return actualValue;
    }
    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }
    public TestStatus getStatus() {
        return status;
    }
    public void setStatus(TestStatus status) {
        this.status = status;
    }
}
