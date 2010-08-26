package org.openl.rules.testmethod;

import org.openl.types.impl.DynamicObject;

/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit {

    private DynamicObject testObj;
    
    // the result of running test unit if exists
    private Object runningResult;
    
    // the exception occurred during running test unit
    private Throwable exception;   
   
    private Object expectedResult;
    
    private Object actualResult;
    
    public static final String DEFAULT_DESCRIPTION = "No Description";

    public TestUnit(DynamicObject obj, Object res, Throwable exception) {
        this.exception = exception;
        this.runningResult = res;
        this.testObj = obj;        
    }

    private void initExpectedResult() {        
        if (exception != null) {
            expectedResult = getFieldValue(TestMethodHelper.EXPECTED_ERROR);
        } else {
            expectedResult = getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
        }
    }
    
    private void initActualResult() {        
        actualResult = exception != null ? exception : runningResult;
    }
    
    /**
     * Gets the expected result.
     * 
     * @return the value of expected result.
     */
    public Object getExpectedResult() {
        if (expectedResult == null) {
            initExpectedResult();
        }
        return expectedResult;
    }
    
    /**
     * Return the result of running current test unit. 
     * 
     * @return exception that occurred during running, if it was. If no, returns the calculated result.
     */
    public Object getActualResult() {
        if (actualResult == null) {
            initActualResult();
        }
        return actualResult;
    }
    
    /**
     * Gets the description field value. 
     * 
     * @return if the description field value presents, return it`s value. In other case return
     * {@link TestUnit#DEFAULT_DESCRIPTION}
     */
    public Object getDescription() {
        Object descr = getFieldValue(TestMethodHelper.DESCRIPTION_NAME);
        
        return descr == null ? DEFAULT_DESCRIPTION : descr;
    }
    
    /**
     * Return the comparasion of the expected result and actual. 
     * 
     * @return see {@link TestUnitResultComparator#getCompareResult(TestUnit)}
     */
    public int compareResult() {        
        return TestUnitResultComparator.getCompareResult(this);
    }
    
    /**
     * Gets the value from the field by it`s fieldName.
     * 
     * @param fieldName
     * @return the value from the field.
     */
    public Object getFieldValue(String fieldName) {
        return testObj.getFieldValue(fieldName);
    }

}
