package org.openl.rules.webstudio.web.test;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;

/**
 * The test suite a benchmark measures: the cases of the test table the page shows.
 *
 * <p>The cases are named by the request the benchmark button posts, so the helper reads them from it.
 */
@Service
@SessionScope
@Deprecated(forRemoval = true)
public class RunTestHelper {

    public TestSuite getTestSuite() {
        String id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        var currentOpenedModule = Boolean.parseBoolean(WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE));

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (id == null) {
            // The request of an action of the table page carries no id of its own; the page it was sent from
            // says which table is open.
            String uri = WebStudioUtils.getWebStudio().getTableUri();
            if (uri != null) {
                id = TableUtils.makeTableId(uri);
            }
        }
        var table = model.getTableById(id);
        if (table == null) {
            return null;
        }
        var uri = table.getUri();
        IOpenMethod method = currentOpenedModule || !model.isProjectCompilationCompleted()
                ? model.getOpenedModuleMethod(uri) : model.getMethod(uri);

        if (method instanceof OpenMethodDispatcher) {
            method = model.getCurrentDispatcherMethod(method, uri);
        }

        TestSuite testSuite;
        if (method instanceof TestSuiteMethod testSuiteMethod) {
            String testRanges = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);
            if (testRanges == null) {
                // Run all test cases of selected test suite
                testSuite = new TestSuite(testSuiteMethod);
            } else {
                // Run only selected test cases of selected test suite
                var indices = testSuiteMethod.getIndices(testRanges);
                testSuite = new TestSuite(testSuiteMethod, indices);
            }
        } else {
            // Only a test table carries the cases a benchmark measures.
            return null;
        }
        return testSuite;
    }
}
