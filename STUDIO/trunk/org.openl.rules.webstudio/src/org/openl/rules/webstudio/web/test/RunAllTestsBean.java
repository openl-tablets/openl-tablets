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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.table.Point;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.BeanResultComparator;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IParameterDeclaration;

/**
 * Request scope managed bean providing logic for 'Run All Tests' page of OpenL
 * Studio.
 */
@ManagedBean
@RequestScoped
public class RunAllTestsBean {
    private TestUnitsResults[] ranResults;

    /**
     * URI of tested table
     */
    private String uri;

    public RunAllTestsBean() {
        uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
        TestResultsHelper.initExplanator();
        testAll();
    }

    private void testAll() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        if (!model.hasTestSuitesToRun() && uri == null) {
            ranResults = model.runAllTests();
        } else {
            List<TestUnitsResults> results = new ArrayList<TestUnitsResults>();
            while(model.hasTestSuitesToRun()){
                TestSuite testSuite = model.popLastTest();
                results.add(model.runTestSuite(testSuite));
            }
            ranResults = new TestUnitsResults[results.size()];
            ranResults = results.toArray(ranResults);            
        }
    }

    public TestUnitsResults[] getRanTests() {
        Comparator<TestUnitsResults> c = new Comparator<TestUnitsResults>() {

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
        Arrays.sort(ranResults, c);

        return ranResults;
    }

    public int getNumberOfTests() {
        return ranResults.length;
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

    /**
     * @return list of errors if there were any during execution of the test. In
     *         other case <code>null</code>.
     */
    public List<OpenLMessage> getErrors(Object objTestUnit) {
        Object result = getActualResultInternal(objTestUnit);
        return TestResultsHelper.getErrors(result);
    }

    /*
     * ------------------------------ EXPECTED VALUE
     * SECTION----------------------------
     */

    /**
     * @return expected result for test
     */
    private Object getExpected(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.getExpectedResult();
    }

    public Object getExpectedParameter(Object objTestUnit) {
        return Collections.singletonList(
                new ParameterWithValueDeclaration("expected", getExpected(objTestUnit), IParameterDeclaration.OUT));
    }

    public ExplanationNumberValue<?> getExplanationValueExpected(Object objTestUnit) {
        Object expected = getExpected(objTestUnit);
        return TestResultsHelper.getExplanationValueResult(expected);
    }

    public int getExpectedExplanatorId(Object objTestUnit) {
        return TestResultsHelper.getExplanatorId(getExplanationValueExpected(objTestUnit));
    }

    /**
     * @return formatted for UI explanation expected value
     */
    public String getFormattedExplanationValueExpected(Object objTestUnit) {
        return TestResultsHelper.format(getExplanationValueExpected(objTestUnit));
    }

    /*
     * ------------------------------ END EXPECTED VALUE
     * ----------------------------
     */

    /*
     * ------------------------------ ACTUAL VALUE
     * SECTION----------------------------
     */

    public Object getActualParameter(Object objTestUnit) {
        return Collections.singletonList(
                new ParameterWithValueDeclaration("actual", getActualResultInternal(objTestUnit), IParameterDeclaration.OUT));
    }

    public String getFormattedExplanationValueActual(Object objTestUnit) {
        return TestResultsHelper.format(getExplanationValueActual(objTestUnit));
    }

    public ExplanationNumberValue<?> getExplanationValueActual(Object objTestUnit) {
        return TestResultsHelper.getExplanationValueResult(getActualResultInternal(objTestUnit));
    }

    public int getActualExplanatorId(Object objTestUnit) {
        return TestResultsHelper.getExplanatorId(getExplanationValueActual(objTestUnit));
    }

    /**
     * @return actual calculated result as Object
     */
    private Object getActualResultInternal(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.getActualResult();
    }

    /*
     * ------------------------------ END ACTUAL VALUE
     * ----------------------------
     */

    /**
     * Compares the expected and actual test results.
     * 
     * @return see {@link TestUnit#compareResult()}
     */
    public int getCompareResult(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.compareResult();
    }

    public ParameterWithValueDeclaration[] getParamDescriptions(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        TestDescription testDescription = testUnit.getTest();
        return testDescription.getExecutionParams();
    }

    public String getUnitDescription(Object objTestUnit) {
        TestUnit testUnit = (TestUnit) objTestUnit;
        return testUnit.getDescription();
    }
    
    public SpreadsheetResult getSpreadsheetResult(Object objTestUnit) {
        return TestResultsHelper.getSpreadsheetResult(getActualResultInternal(objTestUnit));
    }

    public String getFormattedSpreadsheetResult(Object objTestUnit) {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult(objTestUnit);

        if (spreadsheetResult != null) {
            Map<Point, ComparedResult> fieldsCoordinates = getFieldsCoordinates(objTestUnit);
            return new ObjectViewer().displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
        }
        return StringUtils.EMPTY;
    }

    private Map<Point, ComparedResult> getFieldsCoordinates(Object objTestUnit) {
        Map<Point, ComparedResult> fieldsCoordinates = new HashMap<Point, ComparedResult>();
        TestUnit testUnit = (TestUnit) objTestUnit;
        BeanResultComparator comparator = (BeanResultComparator)testUnit.getTestUnitResultComparator().getComparator();
        List<ComparedResult> fieldsToTest = comparator.getFieldsToCompare();

        SpreadsheetOpenClass spreadsheetOpenClass = ((Spreadsheet)testUnit.getTest().getTestedMethod()).getSpreadsheetType();

        if (fieldsToTest != null) {
            for (ComparedResult fieldToTest : fieldsToTest) {
                Point fieldCoordinates = DefaultResultBuilder.getAbsoluteSpreadsheetFieldCoordinates(spreadsheetOpenClass.getField(fieldToTest.getFieldName()));
                if (fieldCoordinates != null) {
                    fieldsCoordinates.put(fieldCoordinates, fieldToTest);
                }
            }
        }
        return fieldsCoordinates;
    }

    public boolean isExpired(){
        return StringUtils.isNotBlank(uri) && (ranResults == null || ranResults.length == 0);
    }

    public String getTestedTableUri(){
        return uri;
    }

    public String getTestTableName(Object testResults){
        return ProjectHelper.createTestName(((TestUnitsResults)testResults).getTestSuite().getTestSuiteMethod());
    }

    public String getTestedTableName(){
        return ProjectHelper.createTestName(WebStudioUtils.getProjectModel().getMethod(uri));
    }
}
