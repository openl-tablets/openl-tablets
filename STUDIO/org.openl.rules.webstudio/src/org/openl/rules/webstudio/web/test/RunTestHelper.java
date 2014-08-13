package org.openl.rules.webstudio.web.test;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;

public final class RunTestHelper {
    private RunTestHelper() {
    }

    public static void addTestSuitesForRun() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        IOpenLTable table = model.getTableById(id);
        if (table != null) {
            String uri = table.getUri();
            IOpenMethod method = model.getMethod(uri);

            if (method instanceof TestSuiteMethod) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

                String testRanges = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);
                String[] testIds = FacesUtils.getRequest().getParameterValues(Constants.REQUEST_PARAM_TEST_ID);
                if (testRanges == null && testIds == null) {
                    // Run all test cases of selected test suite
                    model.addTestSuiteToRun(new TestSuiteWithPreview(testSuiteMethod));
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices;
                    if (testRanges != null) {
                        indices = testSuiteMethod.getIndices(testRanges);
                    } else {
                        indices = new int[testIds.length];
                        for (int i = 0; i < testIds.length; i++) {
                            indices[i] = Integer.parseInt(testIds[i]) - 1;
                        }
                    }
                    model.addTestSuiteToRun(new TestSuiteWithPreview(testSuiteMethod, indices));
                }
            }
        }
    }

}
