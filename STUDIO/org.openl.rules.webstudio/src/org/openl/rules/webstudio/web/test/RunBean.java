package org.openl.rules.webstudio.web.test;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for 'Run Tables' page of WebStudio.
 * TODO: Either make the bean to be request scoped (also store and remove ranResults from session manually) or
 * TODO: resolve memory leak caused by holding same ViewScoped beans in JSF session until user session expires.
 */
@ManagedBean
@ViewScoped
public class RunBean {

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    private TestSuite testSuite;

    private TestUnitsResults results;

    /**
     * ID of tested table
     */
    private String id;

    @PostConstruct
    public void init() {
        testSuite = runTestHelper.getTestSuite();
        id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        if (testSuite != null) {
            ProjectModel model = WebStudioUtils.getProjectModel();
            results = model.runTest(testSuite);
        }
    }

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public String getTableName() {
        return WebStudioUtils.getProjectModel().getTableById(id).getDisplayName();
    }

    public List<ITestUnit> getResults() {
        return results.getTestUnits();
    }

    public boolean isExpired() {
        return StringUtils.isNotBlank(id) && testSuite == null;
    }

    public String getFormattedSpreadsheetResult(ITestUnit unit) {
        Object result = unit.getActualResult();
        if (result instanceof SpreadsheetResult) {
            return ObjectViewer.displaySpreadsheetResult((SpreadsheetResult) result);
        }
        return StringUtils.EMPTY;
    }
}
