package org.openl.rules.testmethod;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.util.StringUtils;

public class TestUnitResultComparator {

    public enum TestStatus {
        TR_EXCEPTION, TR_NEQ, TR_OK;
    }

    private TestResultComparator resultComparator;

    public TestUnitResultComparator(TestResultComparator resultComparator) {
        this.resultComparator = resultComparator;
    }

    public TestResultComparator getComparator() {
        return resultComparator;
    }

    /**
     * Return the comparasion of the expected result and actual one for the test unit.
     *
     * @param testUnit
     * @return <b>0</b> if the expected result is equal to the actual one.<br>
     * <b>1</b> if the expected result is not equal to the actual one.<br>
     * <b>2</b> if the was an exception, during running, that we didn`t expect.
     */
    TestStatus getCompareResult(TestUnit testUnit) {
        if (testUnit.getActualResult() instanceof Throwable) {
            return compareExceptionResult(testUnit);
        }

        if (comapreResult == null) {
            comapreResult = resultComparator.isEqual(testUnit.getExpectedResult(), testUnit.getActualResult());
        }

        if (comapreResult) {
            return TestStatus.TR_OK;
        }

        return TestStatus.TR_NEQ;
    }

    private Boolean comapreResult = null;

    private TestStatus compareExceptionResult(TestUnit testUnit) {
        Throwable rootCause = ExceptionUtils.getRootCause((Throwable)testUnit.getActualResult());
        if (rootCause instanceof OpenLUserRuntimeException) {
            String actualMessage = ((OpenLUserRuntimeException) rootCause).getOriginalMessage();
            Object expectedResult = testUnit.getExpectedResult();
            if (expectedResult != null
                    && !(expectedResult instanceof String)) {
                return TestStatus.TR_NEQ;
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

            if (expectedMessage.equals(actualMessage)) {
                return TestStatus.TR_OK;
            } else {
                return TestStatus.TR_NEQ;
            }
        }

        return TestStatus.TR_EXCEPTION;
    }
}
