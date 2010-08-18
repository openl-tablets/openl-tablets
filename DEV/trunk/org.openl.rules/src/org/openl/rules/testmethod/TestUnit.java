package org.openl.rules.testmethod;

import org.openl.types.impl.DynamicObject;

/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit {

    private DynamicObject testObj;
    private Object res;
    private Throwable exception;    
    
    public static final String NO_DESCRIPTION = "No Description";

    public TestUnit(DynamicObject obj, Object res, Throwable exception) {
        this.exception = exception;
        this.res = res;
        this.testObj = obj;
    }

    public DynamicObject getTestObj() {
        return testObj;
    }
    
    /**     
     * 
     * @return the result of running if exists. see {@link TestUnit#getActualResult()}
     */
    public Object getResult() {
        return res;
    }
    
    /**
     * 
     * @return the exception occurred during running test unit.
     */
    public Throwable getException() {
        return exception;
    }
    
    /**
     * Gets the expected result.
     * 
     * @return the value of expected result.
     */
    public Object getExpectedResult() {
        if (exception != null) {
            return getFieldValue(TestMethodHelper.EXPECTED_ERROR);
        } else {
            return getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
        }
    }
    
    /**
     * Return the result of running current test unit. 
     * 
     * @return exception that occurred during running, if it was. If no, returns the calculated result.
     */
    public Object getActualResult() {
        return exception != null ? exception : res;
    }
    
    /**
     * Gets the description field value. 
     * 
     * @return if the description field value presents, return it`s value. In other case return
     * {@link TestUnit#NO_DESCRIPTION}
     */
    public Object getDescription() {
        Object descr = getFieldValue(TestMethodHelper.DESCRIPTION_NAME);
        
        return descr == null ? NO_DESCRIPTION : descr;
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
