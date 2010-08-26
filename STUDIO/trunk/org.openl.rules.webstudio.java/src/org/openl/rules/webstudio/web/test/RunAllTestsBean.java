package org.openl.rules.webstudio.web.test;

import java.util.List;

import org.ajax4jsf.component.UIRepeat;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.DoubleValue;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.tests.results.RanTestsResults;
import org.openl.rules.ui.tests.results.Test;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.openl.util.print.Formatter;

/**
 * Request scope managed bean providing logic for 'Run All Tests' page of OpenL Studio.
 */
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

        initExplanator();

        testAll(tableUri, testName, unitId);
    }

    private void initExplanator() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR, explanator);
        }
        Explanator.setCurrent(explanator);
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

    public List<OpenLMessage> getErrors() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        Object result = testUnit.getActualResult();
        if (result instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable)result);
        }
        return null;
    }

    public Object getExpected() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        
        return testUnit.getExpectedResult();
    }

    public DoubleValue getDoubleValueExpected() {
        Object expected = getExpected();
        if (expected instanceof DoubleValue) {
            return (DoubleValue) expected;
        }
        return null;
    }

    public Object getResult() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        Object result = testUnit.getActualResult();
        if (result instanceof Throwable) {
            Throwable rootCause = ExceptionUtils.getRootCause((Throwable)result);
            return rootCause.getMessage();
        }
        return result;
    }

    public DoubleValue getDoubleValueResult() {
        Object result = getResult();
        if (result instanceof DoubleValue) {
            return (DoubleValue) result;
        }
        return null;
    }

    public int getResultExplanatorId() {
        DoubleValue doubleValue = getDoubleValueResult();
        return Explanator.getCurrent().getUniqueId(doubleValue);
    }

    public int getExpectedExplanatorId() {
        DoubleValue doubleValue = getDoubleValueExpected();
        return Explanator.getCurrent().getUniqueId(doubleValue);
    }

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
        return Formatter.format(testValue, INamedThing.REGULAR, new StringBuffer()).toString();
    }
    
    public String getUnitDescription() {
        TestUnit testUnit = (TestUnit) testUnits.getRowData();
        return (String)testUnit.getDescription();
    }
    
}
