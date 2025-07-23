package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;
import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for 'Run Tables' page of OpenL Studio.
 */
@Service
@RequestScope
public class RunBean {

    private final RunTestHelper runTestHelper;

    private TestSuite testSuite;

    private TestUnitsResults results;

    /**
     * ID of tested table
     */
    private String id;

    public RunBean(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    @PostConstruct
    public void init() {
        testSuite = runTestHelper.getTestSuite();
        id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        boolean currentOpenedModule = Boolean
                .parseBoolean(WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE));
        if (testSuite != null) {
            results = WebStudioUtils.getProjectModel().runTest(testSuite, currentOpenedModule);
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
        return results != null ? results.getTestUnits() : Collections.emptyList();
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
