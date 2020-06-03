package org.openl.rules.webstudio.web.test;

import java.util.List;

import javax.annotation.PostConstruct;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.MainBean;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean providing logic for 'Run Tables' page of WebStudio.
 */
@Controller
@RequestScope
public class RunBean {

    private final RunTestHelper runTestHelper;

    private final MainBean mainBean;

    private TestSuite testSuite;

    private TestUnitsResults results;

    /**
     * ID of tested table
     */
    private String id;

    public RunBean(RunTestHelper runTestHelper, MainBean mainBean) {
        this.runTestHelper = runTestHelper;
        this.mainBean = mainBean;
    }

    @PostConstruct
    public void init() {
        testSuite = runTestHelper.getTestSuite();
        id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        if (testSuite != null) {
            ProjectModel model = WebStudioUtils.getProjectModel();
            results = model.runTest(testSuite);
        }
    }

    public String getTableName() {
        IOpenLTable table = WebStudioUtils.getProjectModel().getTableById(id);
        if (table == null) {
            return null;
        }
        return table.getDisplayName();
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
            return ObjectViewer.displaySpreadsheetResult((SpreadsheetResult) result, mainBean.getRequestId());
        }
        return StringUtils.EMPTY;
    }
}
