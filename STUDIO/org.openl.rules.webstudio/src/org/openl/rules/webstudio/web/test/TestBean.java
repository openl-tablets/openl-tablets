package org.openl.rules.webstudio.web.test;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.engine.OpenLSystemProperties;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.Point;
import org.openl.rules.testmethod.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.*;

/**
 * Request scope managed bean providing logic for 'Run Tests' page of WebStudio.
 */
@ManagedBean
@ViewScoped
public class TestBean {

    private final Logger log = LoggerFactory.getLogger(TestBean.class);

    public static final Comparator<TestUnitsResults> TEST_COMPARATOR = new Comparator<TestUnitsResults>() {

        public int compare(TestUnitsResults t1, TestUnitsResults t2) {
            if (t2 != null && t1 != null) {
                int cmp = t2.getNumberOfFailures() - t1.getNumberOfFailures();
                if (cmp != 0) {
                    return cmp;
                }
                return t1.getName().compareTo(t2.getName());
            } else {
                return t1 == t2 ? 0 : t1 == null ? 1 : -1;
            }
        }
    };

    private WebStudio studio;
    private TestUnitsResults[] ranResults;
    private boolean runResultsInitialized = false;

    private final static int ALL = -1;

    private final static int DEFAULT_PAGE = 1;
    private int page = DEFAULT_PAGE;
    private int lastPage = DEFAULT_PAGE;

    private int testsPerPage;
    private boolean testsFailuresOnly;
    private int testsFailuresPerTest;
    private boolean showComplexResult;

    /**
     * URI of tested table
     */
    private String uri;

    public TestBean() {
        studio = WebStudioUtils.getWebStudio();
        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        IOpenLTable table = studio.getModel().getTableById(id);
        if (table != null) {
            uri = table.getUri();
        }

        TestResultsHelper.initExplanator();
        // testAll();

        initPagination();
        initFailures();
        initComplexResult();
    }

    private void initPagination() {
        testsPerPage = studio.getTestsPerPage();
        int perPage = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_PERPAGE, testsPerPage);
        if ((perPage == ALL || perPage > 0) && perPage != testsPerPage) {
            testsPerPage = perPage;
        }

        if (ranResults != null && ranResults.length > 0) {
            lastPage = testsPerPage == ALL ? DEFAULT_PAGE
                    : ((int) Math.ceil((double) ranResults.length / testsPerPage));
        }

        int initPage = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_PAGE, DEFAULT_PAGE);
        if (initPage > DEFAULT_PAGE && initPage <= lastPage) {
            page = initPage;
        }
    }

    private void initFailures() {
        testsFailuresOnly = studio.isTestsFailuresOnly();
        String failuresOnlyParameter = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_FAILURES_ONLY);
        boolean failuresOnly = failuresOnlyParameter == null ? testsFailuresOnly
                : Boolean.valueOf(failuresOnlyParameter);
        if (failuresOnly != testsFailuresOnly) {
            testsFailuresOnly = failuresOnly;
        }

        testsFailuresPerTest = studio.getTestsFailuresPerTest();
        int failuresPerTest = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_FAILURES_NUMBER,
                testsFailuresPerTest);
        if ((failuresPerTest == ALL || failuresPerTest > 0) && failuresPerTest != testsFailuresPerTest) {
            testsFailuresPerTest = failuresPerTest;
        }
    }

    private void initComplexResult() {
        showComplexResult = studio.isShowComplexResult();
        String isShowComplexResultParameter = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_COMPLEX_RESULT);
        boolean isShowComplexResult = isShowComplexResultParameter == null ? showComplexResult
                : Boolean.valueOf(isShowComplexResultParameter);
        if (isShowComplexResult != showComplexResult) {
            showComplexResult = isShowComplexResult;
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

        RunTestHelper.addTestSuitesForRun();

        boolean isParallel = OpenLSystemProperties.isRunTestsInParallel(studio.getSystemConfigManager().getProperties());
        if (model.hasTestSuitesToRun()) {
            // Concrete test with cases
            List<TestUnitsResults> results = new ArrayList<TestUnitsResults>();
            while (model.hasTestSuitesToRun()) {
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

    private boolean ranTestsSorted = false;

    public TestUnitsResults[] getRanTests() {
        if (!runResultsInitialized) {
            testAll();
            runResultsInitialized = true;
        }
        if (!ranTestsSorted) {
            Arrays.sort(ranResults, TEST_COMPARATOR);
            ranTestsSorted = true;
        }

        int startPos = (page - 1) * testsPerPage;
        int endPos = startPos + testsPerPage;
        if (endPos >= ranResults.length || testsPerPage == ALL) {
            endPos = ranResults.length;
        }

        return Arrays.copyOfRange(ranResults, startPos, endPos);
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
        if (!runResultsInitialized) {
            testAll();
            runResultsInitialized = true;
        }
        return ranResults != null ? ranResults.length : 0;
    }

    private Integer numberOfFailedTests = null;

    public int getNumberOfFailedTests() {
        if (!runResultsInitialized) {
            testAll();
            runResultsInitialized = true;
        }
        if (numberOfFailedTests == null) {
            int cnt = 0;
            for (TestUnitsResults result : ranResults) {
                if (result != null && result.getNumberOfFailures() > 0) {
                    cnt++;
                }
            }
            numberOfFailedTests = cnt;
        }
        return numberOfFailedTests;
    }

    Integer numberOfFailedTestCases = null;

    public int getNumberOfFailedTestCases() {
        if (!runResultsInitialized) {
            testAll();
            runResultsInitialized = true;
        }
        if (numberOfFailedTestCases == null) {
            int sum = 0;
            for (TestUnitsResults result : ranResults) {
                if (result != null) {
                    sum += result.getNumberOfFailures();
                }
            }
            numberOfFailedTestCases = sum;
        }
        return numberOfFailedTestCases;
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

    /**
     * @return Actual calculated result as Object
     */
    private Object getActualResultInternal(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.getActualResult();
    }

    public boolean isResultThrowable(Object testUnit) {
        return getActualResultInternal(testUnit) instanceof Throwable;
    }

    public boolean isSpreadsheetResult(Object objTestUnit) {
        return getSpreadsheetResult(objTestUnit) != null;
    }

    public boolean isComplexResult(Object objTestUnit) {
        Object actualValue = getActualResultInternal(objTestUnit);
        ParameterWithValueDeclaration param = new ParameterWithValueDeclaration("actual",
                actualValue,
                IParameterDeclaration.OUT);
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
                return ObjectViewer.displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

    public String getTestedTableUri() {
        return uri;
    }

    public String getTestTableName(Object testResults) {
        return ProjectHelper.getTestName(((TestUnitsResults) testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestTableId(Object testResults) {
        String uri = ((TestUnitsResults) testResults).getTestSuite().getUri();
        return TableUtils.makeTableId(uri);
    }

    public String getTestTableInfo(Object testResults) {
        return ProjectHelper.getTestInfo(((TestUnitsResults) testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestedTableName() {
        return ProjectHelper.getTestName(WebStudioUtils.getProjectModel().getMethod(uri));
    }

    public boolean isTest() {
        return StringUtils.isNotBlank(uri) && WebStudioUtils.getProjectModel().getMethod(uri) instanceof TestSuiteMethod;
    }

    public boolean isExpired() {
        return StringUtils.isNotBlank(uri) && runResultsInitialized && (ranResults == null || ranResults.length == 0);
    }

    public int getTestsPerPage() {
        return testsPerPage;
    }

    public boolean isTestsFailuresOnly() {
        return testsFailuresOnly;
    }

    public int getTestsFailuresPerTest() {
        return testsFailuresPerTest;
    }

    public boolean isShowComplexResult() {
        return showComplexResult;
    }

    public void setTestsPerPage(int testsPerPage) {
        this.testsPerPage = testsPerPage;
    }

    public void setTestsFailuresOnly(boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
    }

    public void setTestsFailuresPerTest(int testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
    }

    public void setShowComplexResult(boolean showComplexResult) {
        this.showComplexResult = showComplexResult;
    }
}
