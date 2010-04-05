package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;

import org.ajax4jsf.component.UIRepeat;
import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
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

        Explanator explanator = (Explanator) FacesUtils.getSessionParam("explanator");
        Explanator.setCurrent(explanator);

        tableName = studio.getModel().getDisplayNameFull(tableUri);
        testName = FacesUtils.getRequestParameter("testName");
        if (testName != null) {
            testName = StringTool.decodeURL(testName);
        }
        testId = FacesUtils.getRequestParameter("testID");
        testDescription = FacesUtils.getRequestParameter("testDescr");

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

    public List<OpenLMessage> getErrors() {
        Object result = resultItems.getRowData();

        if (result instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) result);
        }

        return Collections.emptyList();
    }

}
