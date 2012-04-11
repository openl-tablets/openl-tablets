package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;

import org.ajax4jsf.component.UIRepeat;
import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

/**
 * Request scope managed bean providing logic for 'Run TestMethod' page of OpenL Studio.
 */
public class RunTestMethodBean {

    private String tableName;
    private String testName;
    private String testId;
    private String testDescription;

    private Object[] results;

    private UIRepeat resultItems;

    public RunTestMethodBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String tableUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        if (StringUtils.isNotBlank(tableUri)) {
            studio.setTableUri(tableUri);
        } else {
            tableUri = studio.getTableUri();
        }

        initExplanator();

        tableName = studio.getModel().getTable(tableUri).getName();
        testName = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_NAME);
        if (testName != null) {
            testName = StringTool.decodeURL(testName);
        }
        testId = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_ID);
        testDescription = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_DESCRIPTION);

        runTestMethod(tableUri, testId, testName);
    }

    private void initExplanator() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR, explanator);
        }
        Explanator.setCurrent(explanator);
    }

    private void runTestMethod(String tableUri, String testId, String testName) {
        WebStudio studio = WebStudioUtils.getWebStudio();

        Object result =  studio.getModel().runElement(tableUri, testName, testId);

        setResults(result);
    }

    public String getTableName() {
        return tableName;
    }

    public String getTestName() {
        return testName;
    }

    public String getTestId() {
        return testId;
    }

    public String getTestDescription() {
        return testDescription;
    }

    public Object[] getResults() {
        return results;
    }

    private void setResults(Object results) {
        if (results == null) {
            this.results = new Object[0];
        } else if (results.getClass().isArray()) {
            this.results = (Object[]) results;
        } else {
            this.results = new Object[] { results };
        }
    }

    public UIRepeat getResultItems() {
        return resultItems;
    }

    public void setResultItems(UIRepeat resultItems) {
        this.resultItems = resultItems;
    }

    public SpreadsheetResult getSpreadsheetResult() {
        Object result = resultItems.getRowData();
        if (result instanceof SpreadsheetResult) {
            return (SpreadsheetResult) result;
        }
        return null;
    }

    public String getFormattedSpreadsheetResult() {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult();
        if (spreadsheetResult != null) {
            return new ObjectViewer().displayResult(spreadsheetResult);
        }
        return StringUtils.EMPTY;
    }

    public DoubleValue getDoubleValueResult() {
        Object result = resultItems.getRowData();
        if (result instanceof DoubleValue) {
            return (DoubleValue) result;
        }
        return null;
    }

    public int getExplanatorId() {
        DoubleValue doubleValue = getDoubleValueResult();
        return Explanator.getCurrent().getUniqueId(doubleValue);
    }

    public List<OpenLMessage> getErrors() {
        Object result = resultItems.getRowData();

        if (result instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) result);
        }

        return Collections.emptyList();
    }

}
