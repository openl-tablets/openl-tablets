package org.openl.rules.webstudio.web.test;

import java.util.List;

import org.ajax4jsf.component.UIRepeat;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.testmethod.TestResult;
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
        runAllTests(tableUri);
    }

    private void runAllTests(String tableUri) {
        WebStudio studio = WebStudioUtils.getWebStudio();

        Explanator explanator = (Explanator) FacesUtils.getSessionParam("explanator");
        Explanator.setCurrent(explanator);

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

    public List<OpenLMessage> getMessages() {
        TestResult.TestStruct ts = (TestResult.TestStruct) testItems.getRowData();
        Throwable exception = ts.getEx();

        return OpenLMessagesUtils.newMessages(exception);
    }

    public Object getExpected() {
        TestResult.TestStruct ts = (TestResult.TestStruct) testItems.getRowData();
        return ts.getTestObj().getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public Object getResult() {
        TestResult.TestStruct ts = (TestResult.TestStruct) testItems.getRowData();
        return ts.getRes();
    }

    public int getCompareResult() {
        TestResult.TestStruct ts = (TestResult.TestStruct) testItems.getRowData();
        return ts.getEx() != null ? TestResult.TR_EXCEPTION :
            (TestResult.compareResult(getResult(), getExpected())) ? TestResult.TR_OK : TestResult.TR_NEQ;
    }

    public Object getTestValue() {
        TestResult.TestStruct ts = (TestResult.TestStruct) testItems.getRowData();
        String header = (String) headers.getRowData();
        return ts.getTestObj().getFieldValue(header);
    }

}
