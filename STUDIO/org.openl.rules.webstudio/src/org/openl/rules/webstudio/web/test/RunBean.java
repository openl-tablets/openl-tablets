package org.openl.rules.webstudio.web.test;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.engine.OpenLSystemProperties;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.List;

/**
 * Request scope managed bean providing logic for 'Run Tables' page of WebStudio.
 */
@ManagedBean
@ViewScoped
public class RunBean {

    private TestSuite testSuite;

    private TestUnitsResults results;

    /**
     * ID of tested table
     */
    private String id;

    public RunBean() {
        id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        RunTestHelper.addTestSuitesForRun();

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model.hasTestSuitesToRun()) {
            testSuite = model.popLastTest();

            runTestMethod(testSuite);
        }
    }

    private void runTestMethod(TestSuite testSuite) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        boolean isParallel = OpenLSystemProperties.isRunTestsInParallel(studio.getSystemConfigManager().getProperties());
        if (testSuite != null) {
            results = studio.getModel().runTest(testSuite, isParallel);
        }
    }

    public String getTableName() {
        return WebStudioUtils.getProjectModel().getTableById(id).getDisplayName();
    }

    public List<TestUnit> getResults() {
        return results.getTestUnits();
    }

    public boolean isExpired() {
        return StringUtils.isNotBlank(id) && testSuite == null;
    }

    public String getFormattedSpreadsheetResult(TestUnit unit) {
        Object result = unit.getActualResult();
        if (result instanceof SpreadsheetResult) {
            return ObjectViewer.displaySpreadsheetResult((SpreadsheetResult) result);
        }
        return StringUtils.EMPTY;
    }
}
