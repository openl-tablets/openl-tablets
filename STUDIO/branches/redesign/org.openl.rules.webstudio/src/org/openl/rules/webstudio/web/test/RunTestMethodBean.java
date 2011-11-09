package org.openl.rules.webstudio.web.test;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for 'Run TestMethod' page of OpenL
 * Studio.
 */
@ManagedBean
@RequestScoped
public class RunTestMethodBean {

    private TestSuite testSuite;

    private TestUnitsResults results;

    private UIRepeat resultItems;

    public RunTestMethodBean() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        testSuite = model.popLastTest();

        TestResultsHelper.initExplanator();
        runTestMethod(testSuite);
    }

    private void runTestMethod(TestSuite testSuite) {
        WebStudio studio = WebStudioUtils.getWebStudio();

        if (testSuite != null) {
            results = studio.getModel().runTestSuite(testSuite);
        }
    }

    public String getTableName() {
        return testSuite.getName();
    }

    public TestDescription getTestDescription() {
        return getCurrentTest().getTest();
    }

    public List<TestUnit> getResults() {
        return results.getTestUnits();
    }

    public String getUri() {
        return testSuite.getUri();
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
