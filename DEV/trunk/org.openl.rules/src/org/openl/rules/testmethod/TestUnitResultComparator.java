package org.openl.rules.testmethod;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.rules.testmethod.result.TestResultComparator;

public class TestUnitResultComparator {
    
    public static final int TR_EXCEPTION = 2;
    public static final int TR_NEQ = 1;
    public static final int TR_OK = 0;
    
    private TestResultComparator resultComparator;
    
    public TestUnitResultComparator(TestResultComparator resultComparator) {
        this.resultComparator = resultComparator;
    }
    
    /**
     * Return the comparasion of the expected result and actual one for the test unit. 
     * 
     * @param testUnit 
     * @return <b>0</b> if the expected result is equal to the actual one.<br>
     * <b>1</b> if the expected result is not equal to the actual one.<br>
     * <b>2</b> if the was an exception, during running, that we didn`t expect.
     */
    public int getCompareResult(TestUnit testUnit) {
        if (testUnit.getActualResult() instanceof Throwable) {
            return compareExceptionResult(testUnit);
        }
        
        if (compareResult(testUnit.getActualResult(), testUnit.getExpectedResult())) {
            return TR_OK;
        }
        
        return TR_NEQ;
        
    }
    
    @SuppressWarnings("unchecked")
    public boolean compareResult(Object actualResult, Object expectedResult) {

        if (actualResult == expectedResult) {
            return true;
        }

        if (actualResult == null || expectedResult == null) {
            return false;
        }
        
        return resultComparator.compareResult(actualResult, expectedResult);
    }
    
    private int compareExceptionResult(TestUnit testUnit) {
        Throwable rootCause = ExceptionUtils.getRootCause((Throwable)testUnit.getActualResult());
        if (rootCause instanceof OpenLUserRuntimeException) {
            String actualMessage = ((OpenLUserRuntimeException) rootCause).getOriginalMessage();
            Object expectedResult = testUnit.getExpectedResult();
            if (expectedResult != null
                    && !(expectedResult instanceof String)) {
                return TR_NEQ;
            }
            String expectedMessage = (String) expectedResult;

            // OpenL engine recognizes empty cell as 'null' value.
            // When user define 'error' expression with empty string as message
            // test cannot be passed because expected message will be 'null' value.
            // To avoid mentioned above use case we are using the following check.
            // 
            if (expectedMessage == null) {
                expectedMessage = StringUtils.EMPTY;
            }
            
            if (compareResult(actualMessage, expectedMessage)) {
                return TR_OK;
            } else {
                return TR_NEQ;
            }
        }

        return TR_EXCEPTION;
    }
}
