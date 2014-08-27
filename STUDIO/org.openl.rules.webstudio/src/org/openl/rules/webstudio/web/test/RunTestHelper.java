package org.openl.rules.webstudio.web.test;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.TraceTreeBean;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public final class RunTestHelper {

    private Object[] params;

    public void catchParams() {
        this.params = ((InputArgsBean) FacesUtils.getBackingBean("inputArgsBean")).getParams();

        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        ProjectModel model = WebStudioUtils.getProjectModel();
        IOpenLTable table = model.getTableById(id);
        String uri = table.getUri();
        IOpenMethod method =  model.getMethod(uri);
        if (method instanceof OpenMethodDispatcher) {
            method = model.getCurrentDispatcherMethod(method, uri);
        }
        TestDescription testDescription = new TestDescription(method, params);
        TestSuite testSuite = new TestSuiteWithPreview(testDescription);
        WebStudioUtils.getProjectModel().addTestSuiteToRun(testSuite);

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
                if (testRanges == null) {
                    // Run all test cases of selected test suite
                    model.addTestSuiteToRun(new TestSuiteWithPreview(testSuiteMethod));
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices = testSuiteMethod.getIndices(testRanges);
                    model.addTestSuiteToRun(new TestSuiteWithPreview(testSuiteMethod, indices));
                }
            }
        }
    }

}
