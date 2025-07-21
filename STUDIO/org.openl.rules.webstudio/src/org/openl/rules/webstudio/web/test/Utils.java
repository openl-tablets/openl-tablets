package org.openl.rules.webstudio.web.test;

import java.util.Collection;
import java.util.Collections;
import jakarta.servlet.http.HttpSession;

import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.util.Arrays;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class Utils {
    private Utils() {
    }

    private static final String INPUT_ARGS_PARAMETER = "inputArgsParam";

    public static boolean isCollection(IOpenClass openClass) {
        return openClass.getAggregateInfo() != null && openClass.getAggregateInfo().isAggregate(openClass);
    }

    public static String displayNameForCollection(IOpenClass collectionType, boolean isEmpty) {
        StringBuilder builder = new StringBuilder();
        if (isEmpty) {
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(collectionType.getComponentClass().getDisplayName(INamedThing.SHORT));
        return builder.toString();
    }

    static TestUnitsResults[] runTests(String id, String testRanges, boolean currentOpenedModule, HttpSession session) {
        TestUnitsResults[] results;
        ProjectModel model = WebStudioUtils.getWebStudio(session).getModel();

        IOpenLTable table = model.getTableById(id);
        if (table != null) {
            String uri = table.getUri();
            IOpenMethod method = currentOpenedModule ? model.getOpenedModuleMethod(uri) : model.getMethod(uri);

            if (method instanceof TestSuiteMethod) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

                TestSuite testSuite;
                if (testRanges == null) {
                    // Run all test cases of selected test suite
                    testSuite = new TestSuite(testSuiteMethod);
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices = testSuiteMethod.getIndices(testRanges);
                    testSuite = new TestSuite(testSuiteMethod, indices);
                }
                // Concrete test with cases
                results = new TestUnitsResults[1];
                results[0] = model.runTest(testSuite, currentOpenedModule);
            } else {
                // All tests for table
                IOpenMethod[] tests = model.getTestMethods(uri, currentOpenedModule);
                results = runAllTests(model, tests, currentOpenedModule);
            }
        } else {
            // All tests for project
            IOpenMethod[] tests = currentOpenedModule ? model.getOpenedModuleTestMethods() : model.getAllTestMethods();
            results = runAllTests(model, tests, currentOpenedModule);
        }
        return results;
    }

    private static TestUnitsResults[] runAllTests(ProjectModel model,
                                                  IOpenMethod[] tests,
                                                  boolean currentOpenedModule) {
        if (Arrays.isNotEmpty(tests)) {
            TestUnitsResults[] results = new TestUnitsResults[tests.length];
            for (int i = 0; i < tests.length; i++) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) tests[i];
                IOpenMethod testedMethod = testSuiteMethod.getTestedMethod();
                TestSuite testSuite = new TestSuite(testSuiteMethod);
                TestUnitsResults testUnitsResults;
                Collection<IOpenMethod> methods = (testedMethod instanceof OpenMethodDispatcher) ? ((OpenMethodDispatcher) testedMethod)
                        .getCandidates() : Collections.singleton(testedMethod);
                boolean noErrors = true;
                for (IOpenMethod method : methods) {
                    noErrors = noErrors && model.getErrorsByUri(method.getInfo().getSourceUrl()).isEmpty();
                }
                if (currentOpenedModule || noErrors) {
                    testUnitsResults = model.runTest(testSuite, currentOpenedModule);
                } else {
                    testUnitsResults = new TestUnitsResults(testSuite);
                    testUnitsResults.setTestedRulesHaveErrors(true);
                }
                results[i] = testUnitsResults;
            }
            return results;
        }
        return new TestUnitsResults[0];
    }

    public static IDataBase getDb(ProjectModel model, boolean currentOpenedModule) {
        if (model == null) {
            return null;
        }
        CompiledOpenClass compiledOpenClass = currentOpenedModule ? model.getOpenedModuleCompiledOpenClass()
                : model.getCompiledOpenClass();
        IOpenClass moduleClass = compiledOpenClass.getOpenClassWithErrors();
        if (moduleClass instanceof XlsModuleOpenClass) {
            return ((XlsModuleOpenClass) moduleClass).getDataBase();
        }

        return null;
    }

    public static void saveTestToSession(HttpSession session, TestSuite testSuite) {
        session.setAttribute(Utils.INPUT_ARGS_PARAMETER, testSuite);
    }

    public static TestSuite pollTestFromSession(HttpSession session) {
        TestSuite suite = (TestSuite) session.getAttribute(Utils.INPUT_ARGS_PARAMETER);
        session.removeAttribute(Utils.INPUT_ARGS_PARAMETER);
        return suite;
    }
}
