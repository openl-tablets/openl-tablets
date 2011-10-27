package org.openl.rules.testmethod;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.openl.types.impl.DynamicObject;
import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.NumberUtils;

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
	
	private String nameOfExpectedResult;

	public static final String DEFAULT_DESCRIPTION = "No Description";
	
	public TestUnit(DynamicObject obj, Object res, Throwable exception) {
	    this(obj, res, exception, null);
	}
	
	public TestUnit(DynamicObject obj, Object res, Throwable exception, String nameOfExpectedResult) {
		this.exception = exception;
		this.runningResult = res;
		this.testObj = obj;
		if (StringUtils.isNotBlank(nameOfExpectedResult)) {
		    this.nameOfExpectedResult = nameOfExpectedResult;
		} else {
		    this.nameOfExpectedResult = TestMethodHelper.EXPECTED_RESULT_NAME;
		}
	}

	private void initExpectedResult() {
		if (containsException()) {		    
		    Object expectedError = getFieldValue(TestMethodHelper.EXPECTED_ERROR);
		    if (expectedError != null) { // check that it was expected to get an exception
		        expectedResult = expectedError;
		        return;
		    }
		}
		expectedResult = getFieldValue(nameOfExpectedResult);		
	}
	
	protected DynamicObject getTestObject() {
	    return testObj;
	}
	
	protected Object getRunningResult() {
	    return runningResult;
	}
	
	protected Throwable getException() {
	    return exception;
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

		if (NumberUtils.isFloatPointNumber(runningResult) && TestUnitResultComparator.compareResult(runningResult, getExpectedResult())) {
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

    public DynamicObject getTestObj() {
        return testObj;
    }
}
