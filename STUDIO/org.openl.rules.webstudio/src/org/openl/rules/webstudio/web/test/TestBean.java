package org.openl.rules.webstudio.web.test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.Point;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for 'Run Tests' page of OpenL Studio.
 */
@Service
@RequestScope
@Deprecated(forRemoval = true)
public class TestBean {

    private final Logger log = LoggerFactory.getLogger(TestBean.class);

    private static final Comparator<TestUnitsResults> TEST_COMPARATOR = Comparator
            .nullsLast(Comparator.comparingInt(TestUnitsResults::getNumberOfFailures)
                    .reversed()
                    .thenComparing(TestUnitsResults::getName));

    private final WebStudio studio;
    private TestUnitsResults[] ranResults;

    private final static int ALL = -1;

    private final static int DEFAULT_PAGE = 1;
    private int page = DEFAULT_PAGE;
    private int lastPage = DEFAULT_PAGE;

    private int testsPerPage;
    private boolean testsFailuresOnly;
    private int testsFailuresPerTest;
    private boolean showComplexResult;
    private boolean currentOpenedModule;
    private boolean waitForProjectCompilation;

    private boolean ranTestsSorted = false;
    private Integer numberOfFailedTests = null;
    private Integer numberOfFailedTestCases = null;

    /**
     * URI of tested table
     */
    private String uri;

    public TestBean() {
        studio = WebStudioUtils.getWebStudio();
        String id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        IOpenLTable table = studio.getModel().getTableById(id);
        if (table != null) {
            uri = table.getUri();
        }

        initOnlyCurrentModule();

        testAll();

        initPagination();
        initFailures();
        initComplexResult();
    }

    private static int getRequestIntParameter(String name, int defaultValue) {
        String value = WebStudioUtils.getRequestParameter(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void initPagination() {
        testsPerPage = studio.getTestsPerPage();
        int perPage = getRequestIntParameter(Constants.REQUEST_PARAM_PERPAGE, testsPerPage);
        if ((perPage == ALL || perPage > 0) && perPage != testsPerPage) {
            testsPerPage = perPage;
        }

        if (ranResults != null && ranResults.length > 0) {
            lastPage = testsPerPage == ALL ? DEFAULT_PAGE : (int) Math.ceil((double) ranResults.length / testsPerPage);
        }

        int initPage = getRequestIntParameter(Constants.REQUEST_PARAM_PAGE, DEFAULT_PAGE);
        if (initPage > DEFAULT_PAGE && initPage <= lastPage) {
            page = initPage;
        }
    }

    private void initFailures() {
        testsFailuresOnly = studio.isTestsFailuresOnly();
        String failuresOnlyParameter = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_FAILURES_ONLY);
        if (failuresOnlyParameter != null) {
            testsFailuresOnly = Boolean.parseBoolean(failuresOnlyParameter);
        }

        testsFailuresPerTest = studio.getTestsFailuresPerTest();
        int failuresPerTest = getRequestIntParameter(Constants.REQUEST_PARAM_FAILURES_NUMBER, testsFailuresPerTest);
        if (failuresPerTest == ALL || failuresPerTest > 0) {
            testsFailuresPerTest = failuresPerTest;
        }
    }

    private void initComplexResult() {
        showComplexResult = studio.isShowComplexResult();
        String isShowComplexResultParameter = WebStudioUtils
                .getRequestParameter(Constants.REQUEST_PARAM_COMPLEX_RESULT);
        if (isShowComplexResultParameter != null) {
            showComplexResult = Boolean.parseBoolean(isShowComplexResultParameter);
        }
    }

    private void initOnlyCurrentModule() {
        currentOpenedModule = Boolean
                .parseBoolean(WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE));

        waitForProjectCompilation = !currentOpenedModule && studio.getModel().isCompilationInProgress();
    }

    public int getPage() {
        return page;
    }

    public int getLastPage() {
        return lastPage;
    }

    private void testAll() {
        String id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        String testRanges = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);

        if (!isWaitForProjectCompilation()) {
            this.ranResults = Utils.runTests(id, testRanges, currentOpenedModule, WebStudioUtils.getSession());
        } else {
            this.ranResults = new TestUnitsResults[0];
        }
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
        List<ITestUnit> cases = testResult.getTestUnits();
        for (ITestUnit testCase : cases) {
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
        ITestUnit testUnit = (ITestUnit) objTestUnit;
        return testUnit.getActualResult();
    }

    public boolean isResultThrowable(Object testUnit) {
        return getActualResultInternal(testUnit) instanceof Throwable;
    }

    public boolean isComplexResult(Object objTestUnit) {
        Object actualValue = getActualResultInternal(objTestUnit);
        ParameterWithValueDeclaration param = new ParameterWithValueDeclaration("actual", actualValue);
        return !param.getType().isSimple() && !isResultThrowable(objTestUnit);
    }

    public String getFormattedSpreadsheetResult(SpreadsheetResult spreadsheetResult) {
        return spreadsheetResult == null ? ""
                : ObjectViewer.displaySpreadsheetResult(spreadsheetResult
        );
    }

    public String getFormattedSpreadsheetResultFromTestUnit(ITestUnit objTestUnit) {
        Object actualResultInternal = objTestUnit.getActualResult();

        try {
            if (actualResultInternal instanceof SpreadsheetResult) {
                SpreadsheetResult spreadsheetResult = (SpreadsheetResult) actualResultInternal;
                Map<Point, ComparedResult> fieldsCoordinates = getFieldsCoordinates(objTestUnit, spreadsheetResult);
                return ObjectViewer
                        .displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private Map<Point, ComparedResult> getFieldsCoordinates(ITestUnit testUnit, SpreadsheetResult spreadsheetResult) {
        Map<Point, ComparedResult> fieldsCoordinates = new HashMap<>();
        List<ComparedResult> fieldsToTest = testUnit.getComparisonResults();

        if (fieldsToTest != null) {
            Map<String, Point> coordinates = getAbsoluteSpreadsheetFieldCoordinates(spreadsheetResult);

            for (ComparedResult fieldToTest : fieldsToTest) {
                Point fieldCoordinates = coordinates.get(fieldToTest.getFieldName());
                if (fieldCoordinates != null) {
                    fieldsCoordinates.put(fieldCoordinates, fieldToTest);
                }
            }
        }
        return fieldsCoordinates;
    }

    private static Map<String, Point> getAbsoluteSpreadsheetFieldCoordinates(SpreadsheetResult spreadsheetResult) {
        Map<String, Point> absoluteCoordinates = new HashMap<>();

        IGridTable sourceTable = spreadsheetResult.getLogicalTable().getSource();

        String[] rowNames = spreadsheetResult.getRowNames();
        String[] columnNames = spreadsheetResult.getColumnNames();

        for (int i = 0; i < rowNames.length; i++) {
            for (int j = 0; j < columnNames.length; j++) {
                int column = getColumn(sourceTable, j);
                int row = getRow(sourceTable, i);
                ICell cell = sourceTable.getCell(column, row);
                Point absolute = Point.get(cell.getAbsoluteColumn(), cell.getAbsoluteRow());
                String sb = SpreadsheetStructureBuilder.DOLLAR_SIGN + columnNames[j] + SpreadsheetStructureBuilder.DOLLAR_SIGN + rowNames[i];
                absoluteCoordinates.put(sb, absolute);
            }
        }

        return absoluteCoordinates;
    }

    /**
     * Get the column of a field in Spreadsheet.
     *
     * @return column number
     */
    private static int getColumn(IGridTable spreadsheet, int columnFieldNumber) {
        int column = 0;
        int shift = 0;
        // The column 0 contains row headers that's why "<=" instead of "<"
        for (int i = 0; i <= columnFieldNumber; i++) {
            if (shift == 0) {
                shift = spreadsheet.getCell(i, 0).getWidth();
                columnFieldNumber += shift - 1;
                column += shift;
            }
            shift--;
        }
        return column;
    }

    public boolean getCompilationCompleted() {
        return !studio.getModel().isCompilationInProgress();
    }

    public boolean getProjectCompilationCompleted() {
        return studio.getModel().isProjectCompilationCompleted();
    }

    public boolean getCompileThisModuleOnly() {
        return studio.getModel().getModuleInfo().getWebstudioConfiguration().isCompileThisModuleOnly();
    }

    /**
     * Get the row of a field in Spreadsheet.
     *
     * @return row number
     */
    private static int getRow(IGridTable spreadsheet, int rowFieldNumber) {
        int row = 0;
        int shift = 0;
        // The row 0 contains column headers that's why "<=" instead of "<"
        for (int i = 0; i <= rowFieldNumber; i++) {
            if (shift == 0) {
                shift = spreadsheet.getCell(i, 0).getHeight();
                rowFieldNumber += shift - 1;
                row += shift;
            }
            shift--;
        }
        return row;
    }

    public String getTestedTableUri() {
        return uri;
    }

    public String getTestTableName(Object testResults) {
        return TableSyntaxNodeUtils.getTestName(((TestUnitsResults) testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestTableId(Object testResults) {
        String uri = ((TestUnitsResults) testResults).getTestSuite().getUri();
        return TableUtils.makeTableId(uri);
    }

    public String getTestTableInfo(Object testResults) {
        return ProjectHelper.getTestInfo(((TestUnitsResults) testResults).getTestSuite());
    }

    public String getTestedTableName() {
        return TableSyntaxNodeUtils.getTestName(WebStudioUtils.getProjectModel().getMethod(uri));
    }

    public boolean isTest() {
        return StringUtils
                .isNotBlank(uri) && WebStudioUtils.getProjectModel().getMethod(uri) instanceof TestSuiteMethod;
    }

    public boolean isExpired() {
        return StringUtils.isNotBlank(
                uri) && (ranResults == null || ranResults.length == 0) && !isWaitForProjectCompilation() && !currentOpenedModule;
    }

    public int getTestsPerPage() {
        return testsPerPage;
    }

    public boolean isTestsFailuresOnly() {
        return testsFailuresOnly;
    }

    public List<ITestUnit> getTestsToRender(List<ITestUnit> tests, int columnCount) {
        if (tests == null) {
            return null;
        }

        int rows = HTMLRenderer.getMaxNumRowsToDisplay(tests.size(), columnCount);
        if (rows == HTMLRenderer.ALL_ROWS) {
            return tests;
        }

        return tests.subList(0, Math.min(rows, tests.size()));
    }

    public int getMaxNumRowsToDisplay(List<ITestUnit> tests, int columnCount) {
        if (tests == null) {
            return HTMLRenderer.ALL_ROWS;
        }

        return HTMLRenderer.getMaxNumRowsToDisplay(tests.size(), columnCount);
    }

    public int getTestsFailuresPerTest() {
        return testsFailuresPerTest;
    }

    public boolean isShowComplexResult() {
        return showComplexResult;
    }

    public boolean isCurrentOpenedModule() {
        return currentOpenedModule;
    }

    public boolean isWaitForProjectCompilation() {
        return waitForProjectCompilation;
    }
}
