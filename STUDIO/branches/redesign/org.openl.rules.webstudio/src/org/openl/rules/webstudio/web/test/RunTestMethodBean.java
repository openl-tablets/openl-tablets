package org.openl.rules.webstudio.web.test;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for 'Run TestMethod' page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class RunTestMethodBean {

    private String tableName;
    
    private String testName;
    
    private String testId;
    
    private String testDescription;
    
    private String tableUri;

    private Object[] results;

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
        testName = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_NAME);
        if (testName != null) {
            testName = StringTool.decodeURL(testName);
        }
        testId = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_ID);
        testDescription = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_DESCRIPTION);

        runTestMethod(tableUri, testId, testName);
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
    
    public String getUri() {
        return tableUri;
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
    
    public String getStringResult() {
        Object result = resultItems.getRowData();
        return TestResultsHelper.format(result);        
    }

    public SpreadsheetResult getSpreadsheetResult() {
        Object result = resultItems.getRowData();
        return TestResultsHelper.getSpreadsheetResult(result);
    }

    public String getFormattedSpreadsheetResult() {
        SpreadsheetResult spreadsheetResult = getSpreadsheetResult();
        if (spreadsheetResult != null) {
            return new ObjectViewer().displaySpreadsheetResult(spreadsheetResult);
        }
        return StringUtils.EMPTY;
    }

    public ExplanationNumberValue<?> getExplanationValueResult() {
        Object result = resultItems.getRowData();
        return TestResultsHelper.getExplanationValueResult(result);
    }
    
    public String getFormattedExplanationValueResult() {
        return TestResultsHelper.format(getExplanationValueResult());
    }

    public int getExplanatorId() {
        return TestResultsHelper.getExplanatorId(getExplanationValueResult());
    }

    public List<OpenLMessage> getErrors() {
        Object result = resultItems.getRowData();

        return TestResultsHelper.getErrors(result);
    }

}
