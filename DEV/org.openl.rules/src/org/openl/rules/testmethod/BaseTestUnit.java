package org.openl.rules.testmethod;

import static org.openl.rules.testmethod.TestStatus.TR_EXCEPTION;
import static org.openl.rules.testmethod.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestStatus.TR_OK;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.message.OpenLMessage;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
import org.openl.types.IOpenField;
import org.openl.vm.SimpleVM;

public class BaseTestUnit implements ITestUnit {

    @Getter
    private final TestDescription test;
    @Getter(AccessLevel.PACKAGE)
    private final Throwable actualError;
    @Getter
    private final TestStatus resultStatus;
    @Getter
    private final long executionTime;
    // must be increased only through addComparisonResult method
    @Getter
    private final List<ComparedResult> comparisonResults = new ArrayList<>();
    @Getter
    private int numberOfFailedTests;

    BaseTestUnit(TestDescription test, Object res, Throwable error, long executionTime) {
        this.test = test;
        this.executionTime = executionTime;
        var expectedResult = test.getExpectedResult();
        var expectedError = test.getExpectedError();
        if (expectedError != null && expectedResult != null) {
            // Force testcase failure
            this.actualError = new IllegalArgumentException(
                    "Ambiguous expectation in the test case. Two expected result has been declared.");
        } else {
            this.actualError = error;
        }

        this.resultStatus = compareResult(expectedError, expectedResult, res);
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

    /**
     * Gets the description field value.
     *
     * @return if the description field value presents, return it`s value. In other case return
     * {@link ITestUnit#DEFAULT_DESCRIPTION}
     */
    @Override
    public String getDescription() {
        var descr = test.getDescription();
        return descr == null ? DEFAULT_DESCRIPTION : descr;
    }

    /**
     * Return the comparison of the expected result and actual.
     */
    private TestStatus compareResult(Object expectedError, Object expectedResult, Object actualResult) {
        if (actualError != null) {
            return compareError(expectedError, expectedResult, actualResult);
        } else {
            if (expectedError != null) {
                var results = new ComparedResult(null, expectedError, actualResult, TR_NEQ);
                addComparisonResult(results);
                return TR_NEQ;
            } else {
                return compareAndGetResult(expectedResult, actualResult, test.getFields());
            }
        }
    }

    /**
     * Compares the expected error with the error thrown by the tested method.
     */
    private TestStatus compareError(Object expectedError, Object expectedResult, Object actualResult) {
        String oldStyleMessage = switch (expectedError) {
            case null -> null;
            case UserErrorOpenClass.Entry e -> e.get().toString();
            default -> expectedError.toString();
        };
        Throwable rootCause = ExceptionUtils.getRootCause(actualError);
        if (!(rootCause instanceof OpenLUserRuntimeException || rootCause instanceof OutsideOfValidDomainException)) {
            var results = new ComparedResult(null,
                    expectedError == null ? expectedResult : expectedError,
                    rootCause == null ? actualResult : rootCause.getMessage(),
                    TR_EXCEPTION);

            addComparisonResult(results);
            return TR_EXCEPTION;
        }
        if (expectedResult != null) {
            return compareExpectedResultWithError(expectedResult, rootCause.getMessage());
        }
        if (test.isEmptyOrNewStyleErrorDescription()) {
            // to support old behaviour
            return compareMessageAndGetResult(oldStyleMessage, rootCause.getMessage());
        }
        var actual = rootCause instanceof OpenLUserRuntimeException exception
                ? exception.getBody()
                : rootCause.getMessage();
        return compareAndGetResult(expectedError, actual, test.getErrorFields());
    }

    private void addComparisonResult(ComparedResult result) {
        if (TestStatus.TR_OK != result.getStatus()) {
            numberOfFailedTests++;
        }
        comparisonResults.add(result);
    }

    private TestStatus compareMessageAndGetResult(String expectedError, String actualError) {
        var isEqual = Objects.equals(expectedError == null ? "" : expectedError, actualError);
        if (writeFailuresOnly() && isEqual) {
            return TR_OK;
        }
        TestStatus status = isEqual ? TR_OK : TR_NEQ;
        var results = new ComparedResult(null, expectedError, actualError, status);
        addComparisonResult(results);
        return status;
    }

    /**
     * Fails a test case that expects a result, while the tested method throws a user error.
     *
     * <p>Every tested field is reported with its expected value and the message of the error, so a test of some
     * fields of a Spreadsheet shows the expected values of those cells, not the whole expected Spreadsheet.
     */
    private TestStatus compareExpectedResultWithError(Object expectedResult, String actualErrorMessage) {
        var fields = test.getFields();
        if (fields.isEmpty()) {
            addComparisonResult(new ComparedResult(null, expectedResult, actualErrorMessage, TR_NEQ));
        }
        for (IOpenField field : fields) {
            addComparisonResult(new ComparedResult(field.getName(),
                    getFieldValueOrNull(expectedResult, field),
                    actualErrorMessage,
                    TR_NEQ));
        }
        return TR_NEQ;
    }

    private TestStatus compareAndGetResult(Object expectedResult, Object actualResult, List<IOpenField> fieldsToTest) {
        var success = true;

        for (IOpenField field : fieldsToTest) {
            Object actualFieldValue = getFieldValueOrNull(actualResult, field);
            Object expectedFieldValue = getFieldValueOrNull(expectedResult, field);
            success &= isFieldEqual(field, expectedFieldValue, actualFieldValue);
        }
        return success ? TR_OK : TR_NEQ;
    }

    private boolean isFieldEqual(IOpenField field, Object expectedFieldValue, Object actualFieldValue) {
        // Get delta for field if setted
        BigDecimal columnDelta = null;
        if (field instanceof PrecisionFieldChain chain && chain.hasDelta()) {
            columnDelta = chain.getDelta();
        }
        Class<?> clazz = field.getType().getInstanceClass();
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, columnDelta);

        final var equal = comparator.isEqual(expectedFieldValue, actualFieldValue);

        if (writeFailuresOnly() && equal) {
            return true;
        }

        TestStatus status = equal ? TR_OK : TR_NEQ;
        var fieldComparisonResults = new ComparedResult(field.getName(),
                expectedFieldValue,
                actualFieldValue,
                status);
        addComparisonResult(fieldComparisonResults);

        return equal;
    }

    protected boolean writeFailuresOnly() {
        return true;
    }

    private static Object getFieldValueOrNull(Object result, IOpenField field) {
        Object fieldValue = null;
        if (result != null) {
            try {
                var env = new SimpleVM().getRuntimeEnv();
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

    public static final class Builder implements ITestResultBuilder {

        private static final Builder instance = new Builder();

        private Builder() {
            /* NON */
        }

        public static Builder getInstance() {
            return instance;
        }

        @Override
        public ITestUnit build(TestDescription test, Object res, Throwable error, long executionTime) {
            return new BaseTestUnit(test, res, error, executionTime);
        }
    }
}
