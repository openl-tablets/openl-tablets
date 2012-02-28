package org.openl.rules.testmethod;

import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.NumberUtils;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;

/**
 * Representation of the single test unit in the test.
 * 
 */
public class TestUnit {

    private TestDescription test;

    // the result of running test unit if exists
    private Object runningResult;

    // the exception occurred during running test unit
    private Throwable exception;

    private Object expectedResult;

    private Object actualResult;

    public static final String DEFAULT_DESCRIPTION = "No Description";
    
    private TestUnitResultComparator testUnitComparator;

    public TestUnit(TestDescription test, Object res, Throwable exception) {
        this.exception = exception;
        this.runningResult = res;
        this.test = test;
    }

    private void initExpectedResult() {
        if (containsException()) {          
            Object expectedError = test.getExpectedError();
            if (expectedError != null) { // check that it was expected to get an exception
                expectedResult = expectedError;
                return;
            }
        }
        expectedResult = test.getExpectedResult();      
    }
    
    /**
     * Check if the exception occured during the execution. 
     * 
     * @return true if an exception occured during the execution
     */
    private boolean containsException() {
        return exception != null;
    }

    private void initActualResult() {

        if (containsException()) {
            actualResult = exception;
            return;
        } 

        if (NumberUtils.isFloatPointNumber(runningResult) && test.isExpectedResultDefined()) {
            Double result = NumberUtils.convertToDouble(runningResult);
            Double expectedResult = NumberUtils.convertToDouble(getExpectedResult());

            int scale = NumberUtils.getScale(expectedResult);
            Double roundedResult = NumberUtils.roundValue(result, scale);

            if (DoubleValue.class.isAssignableFrom(runningResult.getClass())) {
                ((DoubleValue) runningResult).setValue(roundedResult);
                actualResult = runningResult;
                return;
            }
            
            actualResult = roundedResult;
            return;
        } 
    
        actualResult = runningResult;
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
     * @return exception that occurred during running, if it was. If no, returns
     *         the calculated result.
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
     * @return if the description field value presents, return it`s value. In
     *         other case return {@link TestUnit#DEFAULT_DESCRIPTION}
     */
    public String getDescription() {
        String descr = test.getDescription();
        return descr == null ? DEFAULT_DESCRIPTION : descr;
    }
    
    public void setTestUnitResultComparator(TestUnitResultComparator testUnitComparator) {
        this.testUnitComparator = testUnitComparator;
    }
    
    public TestUnitResultComparator getTestUnitResultComparator() {
        if (testUnitComparator == null) {
            testUnitComparator = new TestUnitResultComparator(TestResultComparatorFactory.getComparator(getActualResult(), getExpectedResult()));
        }
        return testUnitComparator;
    }

    /**
     * Return the comparasion of the expected result and actual.
     * 
     * @return see {@link TestUnitResultComparator#getCompareResult(TestUnit)}
     */
    public int compareResult() {
        return getTestUnitResultComparator().getCompareResult(this);
    }

    /**
     * Gets the value from the field by it`s fieldName.
     * 
     * @param fieldName
     * @return the value from the field.
     * @deprecated It would be better to retrieve test description and use it to get arguments.
     */
    @Deprecated
    public Object getFieldValue(String fieldName) {
        return test.getArgumentValue(fieldName);
    }

    public TestDescription getTest() {
        return test;
    }

    public Object getRunningResult() {
        return runningResult;
    }
    
    public Throwable getException() {
        return exception;
    }
}
