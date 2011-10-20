package org.openl.rules.webstudio.web.test;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;

import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.tests.results.RanTestsResults;
import org.openl.rules.ui.tests.results.Test;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for 'Run All Tests' page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class RunAllTestsBean {

    private RanTestsResults testsResult;

    private UIRepeat testUnits;
    private UIRepeat testDataColumnHeaders;

    public RunAllTestsBean() {
        String tableUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        String testName = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_NAME);
        if (testName != null) {
            testName = StringTool.decodeURL(testName);
        }
        String unitId = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_ID);

        TestResultsHelper.initExplanator();

        testAll(tableUri, testName, unitId);
    }    

    private void testAll(String tableUri, String testName, String unitId) {
        ProjectModel model = WebStudioUtils.getProjectModel();

        if (StringUtils.isNotBlank(unitId)) {
            testsResult =  model.testUnit(tableUri, testName, unitId);
        } else {
            testsResult =  model.testAll(tableUri);
        }
    }

    public Test[] getRanTests() {
        return testsResult.getTests();
    }

    public int getNumberOfTests() {
        return testsResult.getTests().length;
    }

    public int getNumberOfFailedTests() {
        return testsResult.numberOfFailedTests(); 
    }

    public int getNumberOfUnits() {
        return testsResult.totalNumberOfTestUnits();
    }

    public int getNumberOfFailedUnits() {
        return testsResult.totalNumberOfFailures();
    }

    public UIRepeat getTestItems() {
        return testUnits;
    }

    public void setTestItems(UIRepeat testItems) {
        this.testUnits = testItems;
    }

    public UIRepeat getTestDataColumnHeaders() {
        return testDataColumnHeaders;
    }

    public void setTestDataColumnHeaders(UIRepeat testDataColumnHeaders) {
        this.testDataColumnHeaders = testDataColumnHeaders;
    }
    
    /**     
     * @return list of errors if there were any during execution of the test. In other case <code>null</code>.
     */
    public List<OpenLMessage> getErrors() {
        Object result = getActualResultInternal();
        
        return TestResultsHelper.getErrors(result);
    }
    
    /* ------------------------------ EXPECTED VALUE SECTION----------------------------*/
    
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
    
    /* ------------------------------ END EXPECTED VALUE ----------------------------*/
    
    
    /* ------------------------------ ACTUAL VALUE SECTION----------------------------*/
    /**
     * @return formatted actual result
     */
    public String getFormattedActualResult() {        
        Object result = getActualResultInternal();
        if (result instanceof Throwable) {
            Throwable rootCause = ExceptionUtils.getRootCause((Throwable)result);
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
    
    /* ------------------------------ END ACTUAL VALUE ----------------------------*/
    
    /**
     * Compares the expected and actual test results.
     * 
     * @return see {@link TestUnit#compareResult()}
     */
    public int getCompareResult() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        return testUnit.compareResult();
    }

    public Object getTestValue() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        String header = (String) testDataColumnHeaders.getRowData();
        return testUnit.getFieldValue(header);
    }

    public String getFormattedTestValue(){
        Object testValue = getTestValue();
        return TestResultsHelper.format(testValue);
    }
    
    public String getUnitDescription() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        return (String)testUnit.getDescription();
    }
    
}
