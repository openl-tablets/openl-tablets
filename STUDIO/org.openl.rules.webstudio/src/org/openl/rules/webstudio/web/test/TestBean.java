package org.openl.rules.webstudio.web.test;

import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
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
import org.openl.types.IOpenMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request scope managed bean providing logic for 'Run Tests' page of WebStudio.
 * TODO: Either make the bean to be request scoped (also store and remove ranResults from session manually) or
 * TODO: resolve memory leak caused by holding same ViewScoped beans in JSF session until user session expires.
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

    private final static int ALL = -1;

    private final static int DEFAULT_PAGE = 1;
    private int page = DEFAULT_PAGE;
    private int lastPage = DEFAULT_PAGE;

    private int testsPerPage;
    private boolean testsFailuresOnly;
    private int testsFailuresPerTest;
    private boolean showComplexResult;

    private boolean ranTestsSorted = false;
    private Integer numberOfFailedTests = null;
    private Integer numberOfFailedTestCases = null;

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

        testAll();

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
        if (failuresOnlyParameter != null) {
            testsFailuresOnly = Boolean.parseBoolean(failuresOnlyParameter);
        }

        testsFailuresPerTest = studio.getTestsFailuresPerTest();
        int failuresPerTest = FacesUtils.getRequestIntParameter(Constants.REQUEST_PARAM_FAILURES_NUMBER,
                testsFailuresPerTest);
        if (failuresPerTest == ALL || failuresPerTest > 0) {
            testsFailuresPerTest = failuresPerTest;
        }
    }

    private void initComplexResult() {
        showComplexResult = studio.isShowComplexResult();
        String isShowComplexResultParameter = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_COMPLEX_RESULT);
        if (isShowComplexResultParameter != null) {
            showComplexResult = Boolean.parseBoolean(isShowComplexResultParameter);
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

        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        IOpenLTable table = model.getTableById(id);
        if (table != null) {
            String uri = table.getUri();
            IOpenMethod method = model.getMethod(uri);

            if (method instanceof TestSuiteMethod) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

                TestSuite testSuite;
                String testRanges = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);
                if (testRanges == null) {
                    // Run all test cases of selected test suite
                    testSuite = new TestSuiteWithPreview(testSuiteMethod);
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices = testSuiteMethod.getIndices(testRanges);
                    testSuite = new TestSuiteWithPreview(testSuiteMethod, indices);
                }
                // Concrete test with cases
                ranResults = new TestUnitsResults[1];
                ranResults[0] = model.runTest(testSuite);
            } else {
                // All tests for table
                IOpenMethod[] tests = model.getTestMethods(uri);
                ranResults = runAllTests(tests);
            }
        } else {
            // All tests for project
            IOpenMethod[] tests = model.getAllTestMethods();
            ranResults = runAllTests(tests);
        }
    }

    private TestUnitsResults[] runAllTests(IOpenMethod[] tests) {
        if (tests != null) {
            ProjectModel model = WebStudioUtils.getProjectModel();
            TestUnitsResults[] results = new TestUnitsResults[tests.length];
            for (int i = 0; i < tests.length; i++) {
                TestSuiteWithPreview testSuite = new TestSuiteWithPreview((TestSuiteMethod) tests[i]);
                results[i] = model.runTest(testSuite);
            }
            return results;
        }
        return new TestUnitsResults[0];
    }


    public TestUnitsResults[] getRanTests() {
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
        return ranResults != null ? ranResults.length : 0;
    }

    public int getNumberOfFailedTests() {
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

    public int getNumberOfFailedTestCases() {
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

    public boolean isComplexResult(Object objTestUnit) {
        Object actualValue = getActualResultInternal(objTestUnit);
        ParameterWithValueDeclaration param = new ParameterWithValueDeclaration("actual",
                actualValue
        );
        return !param.getType().isSimple() && !isResultThrowable(objTestUnit);
    }

    public String getFormattedSpreadsheetResult(SpreadsheetResult spreadsheetResult) {
        return spreadsheetResult == null ? "" : ObjectViewer.displaySpreadsheetResult(spreadsheetResult);
    }

    public String getFormattedSpreadsheetResultFromTestUnit(TestUnit objTestUnit) {
        Object actualResultInternal = objTestUnit.getActualResult();

        try {
            if (actualResultInternal instanceof SpreadsheetResult) {
                SpreadsheetResult spreadsheetResult = (SpreadsheetResult) actualResultInternal;
                Map<Point, ComparedResult> fieldsCoordinates = getFieldsCoordinates(objTestUnit, spreadsheetResult);
                return ObjectViewer.displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private Map<Point, ComparedResult> getFieldsCoordinates(TestUnit testUnit, SpreadsheetResult spreadsheetResult) {
        Map<Point, ComparedResult> fieldsCoordinates = new HashMap<Point, ComparedResult>();
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
        return ProjectHelper.getTestInfo(((TestUnitsResults) testResults).getTestSuite());
    }

    public String getTestedTableName() {
        return ProjectHelper.getTestName(WebStudioUtils.getProjectModel().getMethod(uri));
    }

    public boolean isTest() {
        return StringUtils.isNotBlank(uri) && WebStudioUtils.getProjectModel().getMethod(uri) instanceof TestSuiteMethod;
    }

    public boolean isExpired() {
        return StringUtils.isNotBlank(uri) && (ranResults == null || ranResults.length == 0);
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
}
