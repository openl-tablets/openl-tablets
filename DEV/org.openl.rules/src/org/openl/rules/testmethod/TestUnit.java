package org.openl.rules.testmethod;

import org.openl.message.OpenLMessage;
import org.openl.rules.testmethod.result.BeanResultComparator;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit {
    private TestDescription test;

    private Object expectedResult;

    private Object actualResult;

    public static final String DEFAULT_DESCRIPTION = "No Description";

    private TestUnitResultComparator testUnitComparator;

    private Integer precision = null;

    public TestUnit(TestDescription test, Object res) {
        this.test = test;
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

    public ParameterWithValueDeclaration getActualParam() {
        return new ParameterWithValueDeclaration("actual", getActualResult());
    }

    public ParameterWithValueDeclaration[] getContextParams(Object objTestResult) {
        return TestUtils.getContextParams(
                ((TestUnitsResults) objTestResult).getTestSuite(), getTest());
    }

    public List<ComparedResult> getResultParams() {
        List<ComparedResult> params = new ArrayList<ComparedResult>();

        Object actual = getActualResult();
        Object expected = getExpectedResult();

        TestResultComparator testComparator = getTestUnitResultComparator().getComparator();
        if (testComparator instanceof BeanResultComparator) {
            if (expected != test.getExpectedError() || test.getExpectedError() == null) {
                List<ComparedResult> results;
                if (actual instanceof Throwable) {
                    results = ((BeanResultComparator) testComparator).getExceptionResults((Throwable) actual, expected);
                } else {
                    results = ((BeanResultComparator) testComparator).getComparisonResults();
                }
                for (ComparedResult comparedResult : results) {
                    comparedResult.setActualValue(new ParameterWithValueDeclaration(
                            comparedResult.getFieldName(), comparedResult.getActualValue()));
                    comparedResult.setExpectedValue(new ParameterWithValueDeclaration(
                            comparedResult.getFieldName(), comparedResult.getExpectedValue()));
                    params.add(comparedResult);
                }
                return params;
            }
        }

        ComparedResult result = new ComparedResult();
        result.setStatus(TestUnitResultComparator.TestStatus.getConstant(compareResult()));
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
     * @return see {@link TestUnitResultComparator#getCompareResult(TestUnit, Double)}
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

    public List<OpenLMessage> getResultMessages() {
        return TestUtils.getUserMessagesAndErrors(getActualResult());
    }

    public List<OpenLMessage> getErrors() {
        return TestUtils.getErrors(getActualResult());
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
