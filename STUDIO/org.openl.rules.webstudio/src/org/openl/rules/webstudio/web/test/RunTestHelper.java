package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.data.IDataBase;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TreeBuildTracer;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;

@ManagedBean
@SessionScoped
public final class RunTestHelper {

    // FIXME last parameters of the test suite should have temporary
    // location(such as Flash scope)
    // but now it placed to session bean due to WebStudio navigation specific
    // TODO move this object to the correct place
    private Object[] params = new Object[0];

    public ITracerObject getTraceObject() {
        catchParams();
        TestSuite testSuite = getTestSuite();
        ProjectModel model = WebStudioUtils.getProjectModel();
        ITracerObject t;
        try {
            t = TreeBuildTracer.initialize();
            model.traceElement(testSuite);
        } finally {
            TreeBuildTracer.destroy();
        }

        return t;
    }

    public void initTrace() {
        ITracerObject root = getTraceObject();

        TraceHelper traceHelper = WebStudioUtils.getTraceHelper();
        traceHelper.cacheTraceTree(root);// Register
    }

    public void catchParams() {
        this.params = ((InputArgsBean) WebStudioUtils.getBackingBean("inputArgsBean")).getParams();
    }

    public void fillBean() {
        ((InputArgsBean) WebStudioUtils.getBackingBean("inputArgsBean")).fillBean();
    }

    public void catchParamsToDownload() {
        catchParams();
        Utils.saveTestToSession(WebStudioUtils.getSession(), getTestSuite());
    }

    public TestSuite getTestSuite() {
        String id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        ProjectModel model = WebStudioUtils.getProjectModel();
        IOpenLTable table = model.getTableById(id);
        if (table == null) {
            return null;
        }
        String uri = table.getUri();
        IOpenMethod method = model.getMethod(uri);
        if (method instanceof OpenMethodDispatcher) {
            method = model.getCurrentDispatcherMethod(method, uri);
        }

        IDataBase db = Utils.getDb(model);

        TestSuite testSuite;
        if (method instanceof TestSuiteMethod) {
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

            String testRanges = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);
            if (testRanges == null) {
                // Run all test cases of selected test suite
                testSuite = new TestSuite(testSuiteMethod);
            } else {
                // Run only selected test cases of selected test suite
                int[] indices = testSuiteMethod.getIndices(testRanges);
                testSuite = new TestSuite(testSuiteMethod, indices);
            }
        } else {
            if (method.getSignature().getNumberOfParameters() > params.length) {
                // View expired
                return null;
            }
            testSuite = new TestSuite(new TestDescription(method, params, db));
        }

        params = new Object[0]; // Reset caught params
        return testSuite;
    }
}
