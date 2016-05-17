package org.openl.rules.testmethod;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.util.StringUtils;

public class TestUnitResultComparator {

    public static enum TestStatus {
        TR_EXCEPTION(2),
        TR_NEQ(1),
        TR_OK(0);

        private int status;

        private TestStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public static TestStatus getConstant(int status) {
            switch (status) {
                case 0:
                    return TR_OK;
                case 1:
                    return TR_NEQ;
                case 2:
                    return TR_EXCEPTION;
                default:
                    throw new OpenlNotCheckedException(String.format("Cant get the constant for compare result for status %d",
                        status));
            }
        }
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
    public int getCompareResult(TestUnit testUnit, Double delta) {
        if (testUnit.getActualResult() instanceof Throwable) {
            return compareExceptionResult(testUnit, delta);
        }

        if (compareResult(testUnit.getActualResult(), testUnit.getExpectedResult(), delta)) {
            return TestStatus.TR_OK.getStatus();
        }

        return TestStatus.TR_NEQ.getStatus();
    }

    private Boolean comapreResult = null;

    public boolean compareResult(Object actualResult, Object expectedResult, Double delta) {

        if (actualResult == expectedResult) {
            return true;
        }

        if (expectedResult == null) {
            if ((actualResult instanceof Object[] && ((Object[]) actualResult).length == 0)
                    || (actualResult instanceof Collection && ((Collection<?>) actualResult).isEmpty())
                    || (actualResult instanceof Map && ((Map<?, ?>) actualResult).isEmpty())
                    || (actualResult instanceof String && ((String) actualResult).isEmpty())) {
                return true;
            }
        }

        if (actualResult == null) {
            return false;
        }

        if (comapreResult == null) {
            comapreResult = resultComparator.compareResult(actualResult, expectedResult, delta);
        }

        return comapreResult;
    }

    private int compareExceptionResult(TestUnit testUnit, Double delta) {
        Throwable rootCause = ExceptionUtils.getRootCause((Throwable)testUnit.getActualResult());
        if (rootCause instanceof OpenLUserRuntimeException) {
            String actualMessage = ((OpenLUserRuntimeException) rootCause).getOriginalMessage();
            Object expectedResult = testUnit.getExpectedResult();
            if (expectedResult != null
                    && !(expectedResult instanceof String)) {
                return TestStatus.TR_NEQ.getStatus();
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

//            if (compareResult(actualMessage, expectedMessage, delta)) {
            if (expectedMessage.equals(actualMessage)) {
                return TestStatus.TR_OK.getStatus();
            } else {
                return TestStatus.TR_NEQ.getStatus();
            }
        }

        return TestStatus.TR_EXCEPTION.getStatus();
    }
}
