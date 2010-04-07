package org.openl.rules.webstudio.web.test;

import java.util.List;

import org.ajax4jsf.component.UIRepeat;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.DoubleValue;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.testmethod.TestResult;
import org.openl.rules.testmethod.TestStruct;
import org.openl.rules.ui.AllTestsRunResult;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean providing logic for 'Run All Tests' page of OpenL Studio.
 */
public class RunAllTestsBean {

    private AllTestsRunResult testsResult;

    private int numberOfTests = 0;
    private int numberOfFailedTests = 0;

    private int numberOfUnits = 0;
    private int numberOfFailedUnits = 0;

    private UIRepeat testItems;
    private UIRepeat headers;

    public RunAllTestsBean() {
        String tableUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        initExplanator();

        runAllTests(tableUri);
    }

    private void initExplanator() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR, explanator);
        }
        Explanator.setCurrent(explanator);
    }

    private void runAllTests(String tableUri) {
        WebStudio studio = WebStudioUtils.getWebStudio();

        testsResult =  studio.getModel().runAllTests(tableUri);

        numberOfTests = testsResult.getTests().length;
        numberOfFailedTests = testsResult.numberOfFailedTests();
        numberOfUnits = testsResult.totalNumberOfTestUnits();
        numberOfFailedUnits = testsResult.totalNumberOfFailures();
    }

    public AllTestsRunResult.Test[] getRanTests() {
        return testsResult.getTests();
    }

    public int getNumberOfTests() {
        return numberOfTests;
    }

    public int getNumberOfFailedTests() {
        return numberOfFailedTests;
    }

    public int getNumberOfUnits() {
        return numberOfUnits;
    }

    public int getNumberOfFailedUnits() {
        return numberOfFailedUnits;
    }

    public UIRepeat getTestItems() {
        return testItems;
    }

    public void setTestItems(UIRepeat testItems) {
        this.testItems = testItems;
    }

    public UIRepeat getHeaders() {
        return headers;
    }

    public void setHeaders(UIRepeat headers) {
        this.headers = headers;
    }

    public List<OpenLMessage> getErrors() {
        TestStruct ts = (TestStruct) testItems.getRowData();
        Throwable exception = ts.getEx();

        return OpenLMessagesUtils.newMessages(exception);
    }

    public Object getExpected() {
        TestStruct ts = (TestStruct) testItems.getRowData();
        return ts.getTestObj().getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public DoubleValue getDoubleValueExpected() {
        Object expected = getExpected();
        if (expected instanceof DoubleValue) {
            return (DoubleValue) expected;
        }
        return null;
    }

    public Object getResult() {
        TestStruct ts = (TestStruct) testItems.getRowData();
        return ts.getRes();
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
        TestStruct ts = (TestStruct) testItems.getRowData();
        return ts.getEx() != null ? TestResult.TR_EXCEPTION :
            (TestResult.compareResult(getResult(), getExpected())) ? TestResult.TR_OK : TestResult.TR_NEQ;
    }

    public Object getTestValue() {
        TestStruct ts = (TestStruct) testItems.getRowData();
        String header = (String) headers.getRowData();
        return ts.getTestObj().getFieldValue(header);
    }

}
