package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for 'Run TestMethod' page of OpenL
 * Studio.
 */
@ManagedBean
@RequestScoped
public class RunTestMethodBean {

    private String tableName;

    private String testId;

    private String tableUri;

    private TestUnit[] results;

    private UIRepeat resultItems;

    public RunTestMethodBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        tableUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        if (StringUtils.isNotBlank(tableUri)) {
            studio.setTableUri(tableUri);
        } else {
            tableUri = studio.getTableUri();
        }

        TestResultsHelper.initExplanator();

        tableName = studio.getModel().getTable(tableUri).getName();
        testId = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_ID);

        runTestMethod(tableUri, testId);
    }

    private void runTestMethod(String tableUri, String testId) {
        WebStudio studio = WebStudioUtils.getWebStudio();

        if (StringUtils.isNotBlank(testId)) {
            results = new TestUnit[] { studio.getModel().runSingleTestUnit(tableUri, testId) };
        } else {
            IOpenMethod method = studio.getModel().getMethod(tableUri);
            if (method instanceof TestSuiteMethod) {
                ArrayList<TestUnit> testUnits = studio.getModel().runTestSuite(tableUri).getTestUnits();
                results = new TestUnit[testUnits.size()];
                results = testUnits.toArray(results);
            } else {
                TestDescription test = new TestDescription(method, new Object[] {});
                results = new TestUnit[] { studio.getModel().runSingleTestUnit(test) };
            }
        }
    }

    public String getTableName() {
        return tableName;
    }

    public String getTestId() {
        return testId;
    }

    public TestDescription getTestDescription() {
        return getCurrentTest().getTest();
    }

    public Object[] getResults() {
        return results;
    }

    public String getUri() {
        return tableUri;
    }

    public UIRepeat getResultItems() {
        return resultItems;
    }

    public void setResultItems(UIRepeat resultItems) {
        this.resultItems = resultItems;
    }

    public TestUnit getCurrentTest() {
        return (TestUnit) resultItems.getRowData();
    }

    public Object getCurrentTestResult() {
        return getCurrentTest().getActualResult();
    }

    public String getStringResult() {
        return TestResultsHelper.format(getCurrentTestResult());
    }

    public SpreadsheetResult getSpreadsheetResult() {
        return TestResultsHelper.getSpreadsheetResult(getCurrentTestResult());
    }

    public String getFormattedSpreadsheetResult() {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult();
        if (spreadsheetResult != null) {
            return new ObjectViewer().displaySpreadsheetResult(spreadsheetResult);
        }
        return StringUtils.EMPTY;
    }

    public ExplanationNumberValue<?> getExplanationValueResult() {
        return TestResultsHelper.getExplanationValueResult(getCurrentTestResult());
    }

    public String getFormattedExplanationValueResult() {
        return TestResultsHelper.format(getExplanationValueResult());
    }

    public int getExplanatorId() {
        return TestResultsHelper.getExplanatorId(getExplanationValueResult());
    }

    public List<OpenLMessage> getErrors() {
        return TestResultsHelper.getErrors(getCurrentTestResult());
    }

}
