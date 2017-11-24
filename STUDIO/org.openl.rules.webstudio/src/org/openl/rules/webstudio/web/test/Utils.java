package org.openl.rules.webstudio.web.test;

import javax.servlet.http.HttpSession;

import org.openl.base.INameSpacedThing;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class Utils {
    static boolean isCollection(IOpenClass openClass) {
        return openClass.getAggregateInfo() != null && openClass.getAggregateInfo().isAggregate(openClass);
    }

    public static String displayNameForCollection(IOpenClass collectionType, boolean isEmpty) {
        StringBuilder builder = new StringBuilder();
        if (isEmpty) {
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(collectionType.getComponentClass().getDisplayName(INameSpacedThing.SHORT));
        return builder.toString();
    }

    static TestUnitsResults[] runTests(String id, String testRanges, HttpSession session) {
        TestUnitsResults[] results;
        ProjectModel model = WebStudioUtils.getWebStudio(session).getModel();

        IOpenLTable table = model.getTableById(id);
        if (table != null) {
            String uri = table.getUri();
            IOpenMethod method = model.getMethod(uri);

            if (method instanceof TestSuiteMethod) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

                TestSuite testSuite;
                if (testRanges == null) {
                    // Run all test cases of selected test suite
                    testSuite = new TestSuiteWithPreview(testSuiteMethod);
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices = testSuiteMethod.getIndices(testRanges);
                    testSuite = new TestSuiteWithPreview(testSuiteMethod, indices);
                }
                // Concrete test with cases
                results = new TestUnitsResults[1];
                results[0] = model.runTest(testSuite);
            } else {
                // All tests for table
                IOpenMethod[] tests = model.getTestMethods(uri);
                results = runAllTests(model, tests);
            }
        } else {
            // All tests for project
            IOpenMethod[] tests = model.getAllTestMethods();
            results = runAllTests(model, tests);
        }
        return results;
    }

    private static TestUnitsResults[] runAllTests(ProjectModel model, IOpenMethod[] tests) {
        if (tests != null) {
            TestUnitsResults[] results = new TestUnitsResults[tests.length];
            for (int i = 0; i < tests.length; i++) {
                TestSuiteWithPreview testSuite = new TestSuiteWithPreview((TestSuiteMethod) tests[i]);
                results[i] = model.runTest(testSuite);
            }
            return results;
        }
        return new TestUnitsResults[0];
    }
}
