package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.table.Point;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.BeanResultComparator;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for 'Run All Tests' page of OpenL
 * Studio.
 */
@ManagedBean
@RequestScoped
public class RunAllTestsBean {
    private TestUnitsResults[] ranResults;

    private UIRepeat testUnits;

    public RunAllTestsBean() {
        TestResultsHelper.initExplanator();
        testAll();
    }

    private void testAll() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        if (!model.hasTestSuitesToRun()) {
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

    public UIRepeat getTestItems() {
        return testUnits;
    }

    public void setTestItems(UIRepeat testItems) {
        this.testUnits = testItems;
    }

    /**
     * @return list of errors if there were any during execution of the test. In
     *         other case <code>null</code>.
     */
    public List<OpenLMessage> getErrors() {
        Object result = getActualResultInternal();

        return TestResultsHelper.getErrors(result);
    }

    /*
     * ------------------------------ EXPECTED VALUE
     * SECTION----------------------------
     */

    /**
     * @return expected result for test
     */
    public Object getExpected() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();

        return testUnit.getExpectedResult();
    }

    /**
     * @return formatted for UI expected result for test
     */
    public String getFormattedExpected() {
        return TestResultsHelper.format(getExpected());
    }

    public ExplanationNumberValue<?> getExplanationValueExpected() {
        Object expected = getExpected();

        return TestResultsHelper.getExplanationValueResult(expected);
    }

    public int getExpectedExplanatorId() {
        return TestResultsHelper.getExplanatorId(getExplanationValueExpected());
    }

    /**
     * @return formatted for UI explanation expected value
     */
    public String getFormattedExplanationValueExpected() {
        return TestResultsHelper.format(getExplanationValueExpected());
    }

    /*
     * ------------------------------ END EXPECTED VALUE
     * ----------------------------
     */

    /*
     * ------------------------------ ACTUAL VALUE
     * SECTION----------------------------
     */
    /**
     * @return formatted actual result
     */
    public String getFormattedActualResult() {
        Object result = getActualResultInternal();
        if (result instanceof Throwable) {
            Throwable rootCause = ExceptionUtils.getRootCause((Throwable) result);
            return rootCause.getMessage();
        }
        // value should be formatted before displaying on UI
        return TestResultsHelper.format(result);
    }

    public String getFormattedExplanationValueActual() {
        return TestResultsHelper.format(getExplanationValueActual());
    }

    public ExplanationNumberValue<?> getExplanationValueActual() {
        return TestResultsHelper.getExplanationValueResult(getActualResultInternal());
    }

    public int getActualExplanatorId() {
        return TestResultsHelper.getExplanatorId(getExplanationValueActual());
    }

    /**
     * @return actual calculated result as Object
     */
    private Object getActualResultInternal() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
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
    public int getCompareResult() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        return testUnit.compareResult();
    }

    public ExecutionParamDescription[] getParamDescriptions() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        TestDescription testDescription = testUnit.getTest();
        return testDescription.getExecutionParams();
    }

    public String getUnitDescription() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        return testUnit.getDescription();
    }
    
    public SpreadsheetResult getSpreadsheetResult() {
        return TestResultsHelper.getSpreadsheetResult(getActualResultInternal());
    }

    public String getFormattedSpreadsheetResult() {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult();
        
        if (spreadsheetResult != null) {
            Map<Point, ComparedResult> fieldsCoordinates = getFieldsCoordinates();
            
            return new ObjectViewer().displaySpreadsheetResult(spreadsheetResult, fieldsCoordinates);
        }
        return StringUtils.EMPTY;
    }

    private Map<Point, ComparedResult> getFieldsCoordinates() {
        Map<Point, ComparedResult> fieldsCoordinates = new HashMap<Point, ComparedResult>();
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
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
    
    

}
