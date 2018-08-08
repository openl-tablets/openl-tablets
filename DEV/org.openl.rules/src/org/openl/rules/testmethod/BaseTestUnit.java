package org.openl.rules.testmethod;

import static org.openl.rules.testmethod.TestStatus.TR_EXCEPTION;
import static org.openl.rules.testmethod.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestStatus.TR_OK;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.message.OpenLMessage;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class BaseTestUnit implements ITestUnit {

    private final TestDescription test;
    private final Throwable actualError;
    private final TestStatus resultStatus;
    private final long executionTime;
    private final List<ComparedResult> comparisonResults = new ArrayList<>();

    BaseTestUnit(TestDescription test, Object res, Throwable error, long executionTime) {
        this.test = test;
        this.executionTime = executionTime;
        Object expectedResult = test.getExpectedResult();
        String expectedError = test.getExpectedError();
        if (expectedError != null && expectedResult != null) {
            // Force testcase failure
            this.actualError = new IllegalArgumentException(
                "Ambiguous expectation in the test case. Two expected result has been declared.");
        } else {
            this.actualError = error;
        }

        this.resultStatus = compareResult(expectedError, expectedResult, res);
    }

    Throwable getActualError() {
        return actualError;
    }

    /**
     * Return the result of running current test case.
     *
     * @return exception that occurred during running, if it was. If no, returns the calculated result.
     */
    @Override
    public Object getActualResult() {
        return actualError;
    }

    @Override
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Gets the description field value.
     *
     * @return if the description field value presents, return it`s value. In other case return
     *         {@link ITestUnit#DEFAULT_DESCRIPTION}
     */
    @Override
    public String getDescription() {
        String descr = test.getDescription();
        return descr == null ? DEFAULT_DESCRIPTION : descr;
    }

    @Override
    public TestStatus getResultStatus() {
        return resultStatus;
    }

    @Override
    public TestDescription getTest() {
        return test;
    }

    @Override
    public List<ComparedResult> getComparisonResults() {
        return comparisonResults;
    }

    /**
     * Return the comparasion of the expected result and actual.
     *
     */
    private TestStatus compareResult(String expectedError, Object expectedResult, Object actualResult) {
        if (actualError != null) {
            Throwable rootCause = ExceptionUtils.getRootCause(actualError);
            if (rootCause instanceof OpenLUserRuntimeException) {
                String actualMessage = rootCause.getMessage();
                return actualMessage.equals(expectedError == null ? "" : expectedError) ? TR_OK : TR_NEQ;
            } else {
                return TR_EXCEPTION;
            }
        } else {
            if (expectedError != null) {
                return TR_NEQ;
            } else {
                return isEqual(expectedResult, actualResult) ? TR_OK : TR_NEQ;
            }
        }
    }

    private boolean isEqual(Object expectedResult, Object actualResult) {
        boolean success = true;

        for (IOpenField field : test.getFields()) {
            Object actualFieldValue = getFieldValueOrNull(actualResult, field);
            Object expectedFieldValue = getFieldValueOrNull(expectedResult, field);
            success &= isFieldEqual(field, expectedFieldValue, actualFieldValue);
        }
        return success;
    }

    private boolean isFieldEqual(IOpenField field, Object expectedFieldValue, Object actualFieldValue) {
        // Get delta for field if setted
        Double columnDelta = null;
        if (field instanceof PrecisionFieldChain) {
            if (((PrecisionFieldChain) field).hasDelta()) {
                columnDelta = ((PrecisionFieldChain) field).getDelta();
            }
        }
        Class<?> clazz = field.getType().getInstanceClass();
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, columnDelta);

        final boolean equal = comparator.isEqual(expectedFieldValue, actualFieldValue);

        if (writeFailuresOnly() && equal) {
            return equal;
        }

        ComparedResult fieldComparisonResults = new ComparedResult();
        fieldComparisonResults.setFieldName(field.getName());
        fieldComparisonResults.setActualValue(actualFieldValue);
        fieldComparisonResults.setExpectedValue(expectedFieldValue);
        fieldComparisonResults.setStatus(equal ? TestStatus.TR_OK : TestStatus.TR_NEQ);
        comparisonResults.add(fieldComparisonResults);

        return equal;
    }

    protected boolean writeFailuresOnly() {
        return true;
    }

    private Object getFieldValueOrNull(Object result, IOpenField field) {
        Object fieldValue = null;
        if (result != null) {
            try {
                IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
                fieldValue = field.get(result, env);
            } catch (Exception ex) {
                fieldValue = ex;
            }
        }
        return fieldValue;
    }

    @Override
    public Object getExpectedResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterWithValueDeclaration[] getContextParams(TestUnitsResults objTestResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComparedResult> getResultParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OpenLMessage> getErrors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterWithValueDeclaration getActualParam() {
        throw new UnsupportedOperationException();
    }

    public static class Builder implements ITestResultBuilder {

        private static Builder instance = new Builder();

        private Builder() { /* NON */ }

        public static Builder getInstance() {
            return instance;
        }

        @Override
        public ITestUnit build(TestDescription test, Object res, Throwable error, long executionTime) {
            return new BaseTestUnit(test, res, error, executionTime);
        }
    }
}
