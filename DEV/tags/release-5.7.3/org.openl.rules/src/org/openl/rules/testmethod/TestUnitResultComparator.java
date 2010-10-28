package org.openl.rules.testmethod;

import java.lang.reflect.Array;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.rules.helpers.NumberUtils;
import org.openl.util.math.MathUtils;

public class TestUnitResultComparator {
    
    private TestUnitResultComparator () {}
        
    public static final int TR_EXCEPTION = 2;
    public static final int TR_NEQ = 1;
    public static final int TR_OK = 0;
    
    /**
     * Return the comparasion of the expected result and actual one for the test unit. 
     * 
     * @param testUnit 
     * @return <b>0</b> if the expected result is equal to the actual one.<br>
     * <b>1</b> if the expected result is not equal to the actual one.<br>
     * <b>2</b> if the was an exception, during running, that we didn`t expect.
     */
    public static int getCompareResult(TestUnit testUnit) {
        if (testUnit.getActualResult() instanceof Throwable) {
            return compareExceptionResult(testUnit);
        }
        
        if (compareResult(testUnit.getActualResult(), testUnit.getExpectedResult())) {
            return TR_OK;
        }
        
        return TR_NEQ;
        
    }

    private static int compareExceptionResult(TestUnit testUnit) {
        Throwable rootCause = ExceptionUtils.getRootCause((Throwable)testUnit.getActualResult());
        if (rootCause instanceof OpenLUserRuntimeException) {
            String message = rootCause.getMessage();
            String expectedMessage = (String) testUnit.getExpectedResult();
            
            // OpenL engine recognizes empty cell as 'null' value.
            // When user define 'error' expression with empty string as message
            // test cannot be passed because expected message will be 'null' value.
            // To avoid mentioned above use case we are using the following check.
            // 
            if (expectedMessage == null) {
                expectedMessage = StringUtils.EMPTY;
            }
            
            if (compareResult(message, expectedMessage)) {
                return TR_OK;
            } else {
                return TR_NEQ;
            }
        }

        return TR_EXCEPTION;
    }
    
    @SuppressWarnings("unchecked")
    public static boolean compareResult(Object res, Object expected) {

        if (res == expected) {
            return true;
        }

        if (res == null || expected == null) {
            return false;
        }
        
        if (NumberUtils.isFloatPointNumber(res)) {
        	Double result = NumberUtils.convertToDouble(res);
        	Double expectedResult = NumberUtils.convertToDouble(expected);
        	
        	return MathUtils.eq(result.doubleValue(), expectedResult.doubleValue());
        }

        if (res instanceof Comparable) {
            return ((Comparable<Object>) res).compareTo(expected) == 0;
        }

        if (res.equals(expected)) {
            return true;
        }

        if (res.getClass().isArray() && expected.getClass().isArray()) {
            return compareArrays(res, expected);
        }

        return false;
    }

    private static boolean compareArrays(Object res, Object expected) {

        int len = Array.getLength(res);
        if (len != Array.getLength(expected)) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            if (!compareResult(Array.get(res, i), Array.get(expected, i))) {
                return false;
            }
        }

        return true;
    }
       
}
