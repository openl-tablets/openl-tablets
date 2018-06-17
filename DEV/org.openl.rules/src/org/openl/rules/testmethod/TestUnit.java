package org.openl.rules.testmethod;

import static org.openl.rules.testmethod.TestStatus.TR_EXCEPTION;
import static org.openl.rules.testmethod.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestStatus.TR_OK;

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
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit {
    private TestDescription test;

    private Object expectedResult;
    private String expectedError;

    private Object actualResult;
    private Throwable actualError;

    public static final String DEFAULT_DESCRIPTION = "No Description";

    private List<ComparedResult> comparisonResults = new ArrayList<ComparedResult>();
    private TestStatus comapreResult;

    private final long executionTime;

    public TestUnit(TestDescription test, Object res, Throwable error, long executionTime) {
        this.test = test;
        this.executionTime = executionTime;
        this.expectedError = test.getExpectedError();
        this.expectedResult = test.getExpectedResult();
        this.actualResult = res;
        this.actualError = error;
        if (expectedError != null && expectedResult != null) {
            // Force testcase failure
            actualError = new IllegalArgumentException(
                "Ambiguous expectation in the test case. Two expected result has been declared.");
        }
    }

    /**
     * Gets the expected result.
     *
     * @return the value of expected result.
     */
    public Object getExpectedResult() {
        return expectedError == null ? expectedResult : expectedError;
    }

    /**
     * Return the result of running current test case.
     *
     * @return exception that occurred during running, if it was. If no, returns the calculated result.
     */
    public Object getActualResult() {
        return actualError == null ? actualResult : actualError;
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

        if (expectedError == null && actualError == null) {
            List<ComparedResult> results = comparisonResults;
            for (ComparedResult comparedResult : results) {
                if (!(comparedResult.getActualValue() instanceof ParameterWithValueDeclaration)) {
                    comparedResult.setActualValue(new ParameterWithValueDeclaration(comparedResult.getFieldName(),
                        comparedResult.getActualValue()));
                }
                if (!(comparedResult.getExpectedValue() instanceof ParameterWithValueDeclaration)) {
                    comparedResult.setExpectedValue(new ParameterWithValueDeclaration(comparedResult.getFieldName(),
                        comparedResult.getExpectedValue()));
                }
                params.add(comparedResult);
            }
            return params;
        }

        ComparedResult result = new ComparedResult();
        result.setStatus(compareResult());
        result.setActualValue(new ParameterWithValueDeclaration("actualResult", getActualResult()));
        result.setExpectedValue(new ParameterWithValueDeclaration("expectedResult", getExpectedResult()));
        params.add(result);

        return params;
    }

    /**
     * Gets the description field value.
     *
     * @return if the description field value presents, return it`s value. In other case return
     *         {@link TestUnit#DEFAULT_DESCRIPTION}
     */
    public String getDescription() {
        String descr = test.getDescription();
        return descr == null ? DEFAULT_DESCRIPTION : descr;
    }

    public List<ComparedResult> getComparisonResults() {
        return comparisonResults;
    }

    /**
     * Return the comparison of the expected result and actual.
     *
     */
    public TestStatus compareResult() {
        if (comapreResult != null) {
            return comapreResult;
        }
        if (actualError != null) {
            Throwable rootCause = ExceptionUtils.getRootCause(actualError);
            if (rootCause instanceof OpenLUserRuntimeException) {
                String actualMessage = rootCause.getMessage();
                comapreResult = actualMessage.equals(expectedError == null ? "" : expectedError) ? TR_OK : TR_NEQ;
            } else {
                comapreResult = TR_EXCEPTION;
            }
        } else {
            if (expectedError != null) {
                comapreResult = TR_NEQ;
            } else {
                comapreResult = isEqual(expectedResult, actualResult) ? TR_OK : TR_NEQ;
            }
        }
        return comapreResult;
    }

    public TestDescription getTest() {
        return test;
    }

    public List<OpenLMessage> getErrors() {
        if (actualError != null) {
            return OpenLMessagesUtils.newErrorMessages(actualError);
        } else {
            return Collections.emptyList();
        }
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
