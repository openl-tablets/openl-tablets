package org.openl.rules.testmethod;

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

    private Integer precision = null;

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

        /*if (NumberUtils.isFloatPointNumber(runningResult) && test.isExpectedResultDefined()) {
            DoubleValue result = NumberUtils.convertToDoubleValue(runningResult);
            Double expectedResult = NumberUtils.convertToDouble(getExpectedResult());

            int scale = NumberUtils.getScale(expectedResult);
            DoubleValue roundedResult = DoubleValue.round(result, scale);
            actualResult = roundedResult;
            return;
        }*/
/*
        // TODO This is a temporary implementation. Delta for doubles compare
        // should be configurable.
        // Implementation should be like this: abs(a - b) < delta.
        // Note: delta is not ulp. ULP cannot be used here.

        // Round the expected and actual values to have only 12 significant
        // digits to fix imprecise comparisons for double values.
        TestResultComparator comparator = o != null ? testUnitComparator.getComparator()
                                                                    : TestResultComparatorFactory.getComparator(runningResult,
                                                                        getExpectedResult());
        if (comparator instanceof OpenLBeanResultComparator) {
            OpenLBeanResultComparator beanComparator = (OpenLBeanResultComparator) comparator;
            actualResult = runningResult; // Cannot clone SpreadsheetResult's

            IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
            for (IOpenField field : beanComparator.getFields()) {
                Object actualField = field.get(actualResult, env);
                if (NumberUtils.isFloatPointNumber(actualField)) {
                    Object expectedField = field.get(getExpectedResult(), env);
                    if (expectedField == null) {
                        continue;
                    }

                    DoubleValue actualValue = NumberUtils.convertToDoubleValue(actualField);
                    DoubleValue expectedValue = NumberUtils.convertToDoubleValue(expectedField);

                    BigDecimal expected = BigDecimal.valueOf(expectedValue.doubleValue());
                    int scaleForDelta = SIGNIFICANT_DIGITS;// - (expected.precision() - expected.scale());

                    DoubleValue roundedResult = DoubleValue.round(actualValue, scaleForDelta);
                    DoubleValue roundedExpected = DoubleValue.round(expectedValue, scaleForDelta);

                    if (actualField instanceof Float) {
                        field.set(actualResult, Float.valueOf(roundedResult.floatValue()), env);
                        field.set(expectedResult, Float.valueOf(roundedExpected.floatValue()), env);
                    } else if (actualField instanceof Double) {
                        field.set(actualResult, Double.valueOf(roundedResult.doubleValue()), env);
                        field.set(expectedResult, Double.valueOf(roundedExpected.doubleValue()), env);
                    } else if (actualField instanceof BigDecimal) {
                        field.set(actualResult, BigDecimal.valueOf(roundedResult.doubleValue()), env);
                        field.set(expectedResult, BigDecimal.valueOf(roundedExpected.doubleValue()), env);
                    } else if (actualField instanceof FloatValue) {
                        field.set(actualResult, DoubleValue.cast(roundedResult, (FloatValue) null), env);
                        field.set(expectedResult, DoubleValue.cast(roundedExpected, (FloatValue) null), env);
                    } else if (actualField instanceof DoubleValue) {
                        field.set(actualResult, roundedResult, env);
                        field.set(expectedResult, roundedExpected, env);
                    } else if (actualField instanceof BigDecimalValue) {
                        field.set(actualResult, DoubleValue.autocast(roundedResult, (BigDecimalValue) null), env);
                        field.set(expectedResult, DoubleValue.autocast(roundedExpected, (BigDecimalValue) null), env);
                    }
                }
            }
            return;
        }
*/
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
     * Return the result of running current test case.
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
        return getTestUnitResultComparator().getCompareResult(this, getDelta());
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

    private Double getDelta() {
        Integer precision = this.precision != null ? this.precision : this.test.getTestTablePrecision();

        if (precision != null) {
            return Math.pow(10.0, -precision);
        }
        return null;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }
}
