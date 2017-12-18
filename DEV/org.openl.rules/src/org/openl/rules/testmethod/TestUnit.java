package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
import org.openl.types.IOpenField;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

import static org.openl.rules.testmethod.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestStatus.TR_OK;
import static org.openl.rules.testmethod.TestStatus.TR_EXCEPTION;
/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit {
    private TestDescription test;

    private Object expectedResult;

    private Object actualResult;

    public static final String DEFAULT_DESCRIPTION = "No Description";

    private List<ComparedResult> comparisonResults = new ArrayList<ComparedResult>();
    private TestStatus comapreResult;

    private final long executionTime;

    public TestUnit(TestDescription test, Object res, long executionTime) {
        this.test = test;
        this.executionTime = executionTime;
        initExpectedResult(test);
        this.actualResult = res;
    }

    private void initExpectedResult(TestDescription test) {
        Object expectedError = test.getExpectedError();
        if (expectedError != null) { // check that it was expected to get an exception
            expectedResult = expectedError;
        } else {
            expectedResult = test.getExpectedResult();
        }
    }

    /**
     * Gets the expected result.
     *
     * @return the value of expected result.
     */
    public Object getExpectedResult() {
        return expectedResult;
    }

    /**
     * Return the result of running current test case.
     *
     * @return exception that occurred during running, if it was. If no, returns
     *         the calculated result.
     */
    public Object getActualResult() {
        return actualResult;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public ParameterWithValueDeclaration getActualParam() {
        return new ParameterWithValueDeclaration("actual", getActualResult());
    }

    public ParameterWithValueDeclaration[] getContextParams(TestUnitsResults objTestResult) {
        return TestUtils.getContextParams(objTestResult.getTestSuite(), test);
    }

    public List<ComparedResult> getResultParams() {
        List<ComparedResult> params = new ArrayList<ComparedResult>();

        Object actual = getActualResult();
        Object expected = getExpectedResult();

            String expectedError = test.getExpectedError();
            if (expectedError == null) {
                List<ComparedResult> results = comparisonResults;
                for (ComparedResult comparedResult : results) {
                    if (!(comparedResult.getActualValue() instanceof ParameterWithValueDeclaration)) {
                        comparedResult.setActualValue(new ParameterWithValueDeclaration(
                                comparedResult.getFieldName(), comparedResult.getActualValue()));
                    }
                    if (!(comparedResult.getExpectedValue() instanceof ParameterWithValueDeclaration)) {
                        comparedResult.setExpectedValue(new ParameterWithValueDeclaration(
                                comparedResult.getFieldName(), comparedResult.getExpectedValue()));
                    }
                    params.add(comparedResult);
                }
                return params;
        }

        ComparedResult result = new ComparedResult();
        result.setStatus(compareResult());
        result.setActualValue(new ParameterWithValueDeclaration("actual", actual));
        result.setExpectedValue(new ParameterWithValueDeclaration("expected", expected));
        params.add(result);

        return params;
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

    public List<ComparedResult> getComparisonResults() {
        return comparisonResults;
    }

    /**
     * Return the comparasion of the expected result and actual.
     *
     */
    public TestStatus compareResult() {
        if (comapreResult != null) {
            return comapreResult;
        }
        Object actualResult = getActualResult();
        Object expectedResult = getExpectedResult();
        if (actualResult instanceof Throwable) {
            Throwable rootCause = ExceptionUtils.getRootCause((Throwable) actualResult);
            if (rootCause instanceof OpenLUserRuntimeException) {
                // OpenL engine recognizes empty cell as 'null' value.
                // When user define 'error' expression with empty string as message
                // test cannot be passed because expected message will be 'null' value.
                // To avoid mentioned above use case we are using the following check.
                //
                if (expectedResult == null) {
                    expectedResult = StringUtils.EMPTY;
                }

                String actualMessage = rootCause.getMessage();
                comapreResult = expectedResult.equals(actualMessage) ? TR_OK : TR_NEQ;
            } else {
                comapreResult = TR_EXCEPTION;
            }
        } else {
            comapreResult = isEqual(expectedResult, actualResult) ? TR_OK : TR_NEQ;
        }

        return comapreResult;
    }

    public TestDescription getTest() {
        return test;
    }

    public List<OpenLMessage> getErrors() {
        Object error = getActualResult();
        if (error instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) error);
        }

        return Collections.emptyList();
    }

    private boolean isEqual(Object expectedResult, Object actualResult) {
        boolean success = true;
        comparisonResults = new ArrayList<>();

        for (IOpenField field : test.getFields()) {
            Object actualFieldValue = getFieldValueOrNull(actualResult, field);
            Object expectedFieldValue = getFieldValueOrNull(expectedResult, field);
            // Get delta for field if setted
            Double columnDelta = null;
            if (field instanceof PrecisionFieldChain) {
                if (((PrecisionFieldChain) field).hasDelta()) {
                    columnDelta = ((PrecisionFieldChain) field).getDelta();
                }
            }
            Class<?> clazz = field.getType().getInstanceClass();
            TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, columnDelta);
            boolean equal = comparator.isEqual(expectedFieldValue, actualFieldValue);
            success = success && equal;

            ComparedResult fieldComparisonResults = new ComparedResult();
            fieldComparisonResults.setFieldName(field.getName());
            fieldComparisonResults.setActualValue(actualFieldValue);
            fieldComparisonResults.setExpectedValue(expectedFieldValue);
            fieldComparisonResults.setStatus(equal ? TestStatus.TR_OK : TestStatus.TR_NEQ);
            comparisonResults.add(fieldComparisonResults);
        }
        return success;

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
}
