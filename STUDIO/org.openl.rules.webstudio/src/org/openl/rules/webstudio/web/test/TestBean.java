package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.table.Point;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.testmethod.result.BeanResultComparator;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IParameterDeclaration;

/**
 * Request scope managed bean providing logic for 'Run Tests' page of WebStudio.
 */
@ManagedBean
@RequestScoped
public class TestBean {

    private final Log LOG = LogFactory.getLog(TestBean.class);

    public static final Comparator<TestUnitsResults> TEST_COMPARATOR = new Comparator<TestUnitsResults>() {

        public int compare(TestUnitsResults t1, TestUnitsResults t2) {
            if (t2 != null && t1 != null) {
                int cmp = t2.getNumberOfFailures() - t1.getNumberOfFailures();
                if (cmp != 0) {
                    return cmp;
                }
            }

            return t1.getName().compareTo(t2.getName());
        }
    };

    private WebStudio studio;
    private TestUnitsResults[] ranResults;

    private final static int ALL = -1;

    private final static int DEFAULT_PAGE = 1;
    private int page = DEFAULT_PAGE;
    private int lastPage = DEFAULT_PAGE;

    /**
     * URI of tested table
     */
    private String uri;

    public TestBean() {
        studio = WebStudioUtils.getWebStudio();
        uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        TestResultsHelper.initExplanator();
        testAll();

        initPagination();
        initFailures();
        initComplexResult();
    }

    private void initPagination() {
        int defaultPerPage = studio.getTestsPerPage();
        int perPage = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_PERPAGE, defaultPerPage);
        if ((perPage == ALL || perPage > 0) && perPage != defaultPerPage) {
            studio.setTestsPerPage(perPage);
        }

        if (ranResults != null && ranResults.length > 0) {
            lastPage = studio.getTestsPerPage() == ALL ? DEFAULT_PAGE
                    : ((int) Math.ceil((double) ranResults.length / studio.getTestsPerPage()));
        }

        int initPage = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_PAGE, DEFAULT_PAGE);
        if (initPage > DEFAULT_PAGE && initPage <= lastPage) {
            page = initPage;
        }
    }

    private void initFailures() {
        boolean defaultFailuresOnly = studio.isTestsFailuresOnly();
        boolean failuresOnly = Boolean.valueOf(
                FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_FAILURES_ONLY));
        if (failuresOnly != defaultFailuresOnly) {
            studio.setTestsFailuresOnly(failuresOnly);
        }

        int defaultFailuresPerTest = studio.getTestsFailuresPerTest();
        int failuresPerTest = FacesUtils.getRequestIntParameter(
                Constants.REQUEST_PARAM_FAILURES_NUMBER, defaultFailuresPerTest);
        if ((failuresPerTest == ALL || failuresPerTest > 0) && failuresPerTest != defaultFailuresPerTest) {
            studio.setTestsFailuresPerTest(failuresPerTest);
        }
    }

    private void initComplexResult() {
        boolean defaultShowComplexResult = studio.isShowComplexResult();
        boolean showComplexResult = Boolean.valueOf(
                FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_COMPLEX_RESULT));
        if (showComplexResult != defaultShowComplexResult) {
            studio.setShowComplexResult(showComplexResult);
        }
    }

    public int getPage() {
        return page;
    }

    public int getLastPage() {
        return lastPage;
    }

    private void testAll() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        boolean isParallel = OpenLSystemProperties.isRunTestsInParallel(studio.getSystemConfigManager().getProperties());
        if (model.hasTestSuitesToRun()) {
            // Concrete test with cases
            List<TestUnitsResults> results = new ArrayList<TestUnitsResults>();
            while (model.hasTestSuitesToRun()){
                TestSuite test = model.popLastTest();
                results.add(model.runTest(test, isParallel));
            }
            ranResults = new TestUnitsResults[results.size()];
            ranResults = results.toArray(ranResults);

        } else {
            if (uri != null) {
                // All tests for table or concrete test
                ranResults = model.runAllTests(uri);
            } else {
                // All module tests
                ranResults = model.runAllTests();
            }
        }
    }

    public TestUnitsResults[] getRanTests() {
        Arrays.sort(ranResults, TEST_COMPARATOR);

        int testsPerPage = studio.getTestsPerPage();

        int startPos = (page - 1) * testsPerPage;
        int endPos = startPos + testsPerPage;
        if (endPos >= ranResults.length || testsPerPage == ALL) {
            endPos = ranResults.length;
        }

        return Arrays.copyOfRange(ranResults, startPos, endPos);
    }

    public List<TestUnit> getFilteredTestCases(TestUnitsResults testResult) {
        List<TestUnit> cases = testResult.getTestUnits();

        if (studio.isTestsFailuresOnly()) {
            List<TestUnit> failedCases = new ArrayList<TestUnit>();
            for (TestUnit testUnit : cases) {
                if (testUnit.compareResult() != 0 // Failed case
                        && (failedCases.size() < studio.getTestsFailuresPerTest()
                                || studio.getTestsFailuresPerTest() == ALL)) {
                    failedCases.add(testUnit);
                }
            }
            return failedCases;
        }

        return cases;
    }

    public boolean hasComplexResults(TestUnitsResults testResult) {
        List<TestUnit> cases = testResult.getTestUnits();
        for (TestUnit testCase : cases) {
            if (isComplexResult(testCase)) {
                return true;
            }
        }
        return false;
    }

    public int getNumberOfTests() {
        return ranResults!= null ? ranResults.length : 0;
    }

    public int getNumberOfFailedTests() {
        int cnt = 0;
        for (TestUnitsResults result : ranResults) {
            if (result != null && result.getNumberOfFailures() > 0) {
                cnt++;
            }
        }
        return cnt;
    }

    public int getNumberOfFailedTestCases() {
        int sum = 0;
        for (TestUnitsResults result : ranResults) {
            if (result != null) {
                sum += result.getNumberOfFailures();
            }
        }
        return sum;
    }

    /**
     * @return list of errors if there were any during execution of the test. In
     *         other case <code>null</code>.
     */
    public List<OpenLMessage> getErrors(Object objTestUnit) {
        Object result = getActualResultInternal(objTestUnit);
        return TestResultsHelper.getErrors(result);
    }

    public List<OpenLMessage> getResultMessages(Object objTestUnit) {
        Object result = getActualResultInternal(objTestUnit);
        return TestResultsHelper.getUserMessagesAndErrors(result);
    }

    public List<ComparedResult> getResultParams(Object objTestCase) {
        List<ComparedResult> params = new ArrayList<ComparedResult>();

        TestUnit testCase = ((TestUnit) objTestCase);

        Object actual = testCase.getActualResult();
        Object expected = testCase.getExpectedResult();

        if (!(actual instanceof Throwable)) {
            TestResultComparator testComparator = testCase.getTestUnitResultComparator().getComparator();
            if (testComparator instanceof BeanResultComparator) {
                List<ComparedResult> results = ((BeanResultComparator) testComparator).getComparisonResults();
                for (ComparedResult comparedResult : results) {
                    comparedResult.setActualValue(new ParameterWithValueDeclaration(
                            comparedResult.getFieldName(), comparedResult.getActualValue(), IParameterDeclaration.OUT));
                    comparedResult.setExpectedValue(new ParameterWithValueDeclaration(
                            comparedResult.getFieldName(), comparedResult.getExpectedValue(), IParameterDeclaration.OUT));
                    params.add(comparedResult);
                }
                return params;
            }
        }

        ComparedResult result = new ComparedResult();
        result.setStatus(TestStatus.TR_OK.getConstant(testCase.compareResult()));
        result.setActualValue(new ParameterWithValueDeclaration("actual", actual, IParameterDeclaration.OUT));
        result.setExpectedValue(new ParameterWithValueDeclaration("expected", expected, IParameterDeclaration.OUT));
        params.add(result);

        return params;
    }

    public String formatExplanationValue(Object value) {
        return TestResultsHelper.format(TestResultsHelper.getExplanationValueResult(value));
    }

    public boolean isExplanationValue(Object value) {
        return TestResultsHelper.getExplanationValueResult(value) != null;
    }

    public int getExplanatorId(Object value) {
        return TestResultsHelper.getExplanatorId((ExplanationNumberValue<?>) value);
    }

    public Object getActualResult(Object objTestUnit) {
        return Collections.singletonList(new ParameterWithValueDeclaration(
                "actual", getActualResultInternal(objTestUnit), IParameterDeclaration.OUT));
    }

    /**
     * @return Actual calculated result as Object
     */
    private Object getActualResultInternal(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.getActualResult();
    }

    public ParameterWithValueDeclaration[] getContextParams(Object objTestResult, Object objTestCase) {
        return TestUtils.getContextParams(
                ((TestUnitsResults) objTestResult).getTestSuite(),
                ((TestUnit) objTestCase).getTest());
    }

    public ParameterWithValueDeclaration[] getParamDescriptions(Object objTestCase) {
        TestUnit testCase = (TestUnit) objTestCase;
        TestDescription testCaseDescription = testCase.getTest();
        return testCaseDescription.getExecutionParams();
    }

    public boolean isResultThrowable(Object testUnit) {
        return getActualResultInternal(testUnit) instanceof Throwable;
    }

    public boolean isSpreadsheetResult(Object objTestUnit) {
        return getSpreadsheetResult(objTestUnit) != null;
    }

    public boolean isComplexResult(Object objTestUnit) {
        Object actualValue = getActualResultInternal(objTestUnit);
        ParameterWithValueDeclaration param = new ParameterWithValueDeclaration(
                "actual", actualValue, IParameterDeclaration.OUT);
        return !param.getType().isSimple() && !isResultThrowable(objTestUnit);
    }

    public SpreadsheetResult getSpreadsheetResult(Object objTestUnit) {
        return TestResultsHelper.getSpreadsheetResult(getActualResultInternal(objTestUnit));
    }

    public String getFormattedSpreadsheetResult(Object objTestUnit) {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult(objTestUnit);

        try {
            if (spreadsheetResult != null) {
                Map<Point, ComparedResult> fieldsCoordinates = getFieldsCoordinates(objTestUnit, spreadsheetResult);
                return new ObjectViewer().displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }
        return StringUtils.EMPTY;
    }

    private Map<Point, ComparedResult> getFieldsCoordinates(Object objTestUnit, SpreadsheetResult spreadsheetResult) {
        Map<Point, ComparedResult> fieldsCoordinates = new HashMap<Point, ComparedResult>();
        TestUnit testUnit = (TestUnit) objTestUnit;
        TestResultComparator testUnitResultComparator = testUnit.getTestUnitResultComparator().getComparator();
        if (!(testUnitResultComparator instanceof BeanResultComparator)) {
            return fieldsCoordinates;
        }

        BeanResultComparator comparator = (BeanResultComparator) testUnitResultComparator;
        List<ComparedResult> fieldsToTest = comparator.getComparisonResults();

        if (fieldsToTest != null) {
            Map<String, Point> coordinates = DefaultResultBuilder.getAbsoluteSpreadsheetFieldCoordinates(spreadsheetResult);

            for (ComparedResult fieldToTest : fieldsToTest) {
                Point fieldCoordinates = coordinates.get(fieldToTest.getFieldName());
                if (fieldCoordinates != null) {
                    fieldsCoordinates.put(fieldCoordinates, fieldToTest);
                }
            }
        }
        return fieldsCoordinates;
    }

    public String getTestedTableUri(){
        return uri;
    }

    public String getTestTableName(Object testResults) {
        return ProjectHelper.getTestName(((TestUnitsResults) testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestTableInfo(Object testResults) {
        return ProjectHelper.getTestInfo(((TestUnitsResults) testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestedTableName() {
        return ProjectHelper.getTestName(WebStudioUtils.getProjectModel().getMethod(uri));
    }

    public boolean isTest() {
        return StringUtils.isNotBlank(uri)
                && WebStudioUtils.getProjectModel().getMethod(uri) instanceof TestSuiteMethod;
    }

    public boolean isExpired(){
        return StringUtils.isNotBlank(uri) && (ranResults == null || ranResults.length == 0);
    }

}
