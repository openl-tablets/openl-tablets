package org.openl.rules.webstudio.projects.tests.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.util.Arrays;
import org.openl.types.IOpenMethod;

@Component
public class TestsExecutorServiceImpl implements TestsExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<List<TestUnitsResults>> runAll(ProjectTestsExecutionProgressListener listener, ProjectModel projectModel, boolean currentOpenedModule) {
        var testMethods = currentOpenedModule ? projectModel.getOpenedModuleTestMethods() : projectModel.getAllTestMethods();
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        runAllTests(listener, executionResult, projectModel, testMethods, currentOpenedModule);
        return CompletableFuture.completedFuture(executionResult);
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<List<TestUnitsResults>> runAllForTable(ProjectTestsExecutionProgressListener listener, ProjectModel projectModel, IOpenLTable table, boolean currentOpenedModule) {
        String uri = table.getUri();
        IOpenMethod method = currentOpenedModule ? projectModel.getOpenedModuleMethod(uri) : projectModel.getMethod(uri);
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        if (method instanceof TestSuiteMethod) {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(executionResult);
        }
        TestSuiteMethod[] testMethods = projectModel.getTestMethods(uri, currentOpenedModule);
        runAllTests(listener, executionResult, projectModel, testMethods, currentOpenedModule);
        return CompletableFuture.completedFuture(executionResult);
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<List<TestUnitsResults>> runSingle(ProjectTestsExecutionProgressListener listener, ProjectModel projectModel, IOpenLTable table, String testRanges, boolean currentOpenedModule) {
        String uri = table.getUri();
        IOpenMethod method = currentOpenedModule ? projectModel.getOpenedModuleMethod(uri) : projectModel.getMethod(uri);
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        if (method instanceof TestSuiteMethod testSuiteMethod) {
            TestSuite testSuite;
            if (testRanges == null) {
                // Run all test cases of selected test suite
                testSuite = new TestSuite(testSuiteMethod);
            } else {
                // Run only selected test cases of selected test suite
                int[] indices = testSuiteMethod.getIndices(testRanges);
                testSuite = new TestSuite(testSuiteMethod, indices);
            }
            var unitsResult = projectModel.runTest(testSuite, currentOpenedModule);
            listener.onTestUnitExecuted(unitsResult);
            executionResult.add(unitsResult);
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(executionResult);
        } else {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(executionResult);
        }
    }

    private void runAllTests(ProjectTestsExecutionProgressListener listener,
                             List<TestUnitsResults> executionResult,
                             ProjectModel model,
                             TestSuiteMethod[] tests,
                             boolean currentOpenedModule) {
        if (Arrays.isEmpty(tests)) {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return;
        }

        boolean interrupted = false;
        for (TestSuiteMethod testSuiteMethod : tests) {
            if (Thread.currentThread().isInterrupted()) {
                listener.onStatusChanged(TestExecutionStatus.INTERRUPTED);
                interrupted = true;
                break;
            }
            TestUnitsResults testUnitsResults = runSingleTest(model, testSuiteMethod, currentOpenedModule);
            executionResult.add(testUnitsResults);
            listener.onTestUnitExecuted(testUnitsResults);
        }
        if (!interrupted) {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
        }
    }

    private TestUnitsResults runSingleTest(ProjectModel model, TestSuiteMethod testSuiteMethod, boolean currentOpenedModule) {
        IOpenMethod testedMethod = testSuiteMethod.getTestedMethod();
        TestSuite testSuite = new TestSuite(testSuiteMethod);
        TestUnitsResults testUnitsResults;
        Collection<IOpenMethod> methods = (testedMethod instanceof OpenMethodDispatcher dispatcher)
                ? dispatcher.getCandidates()
                : Collections.singleton(testedMethod);
        boolean noErrors = true;
        for (IOpenMethod method : methods) {
            if (!model.getErrorsByUri(method.getInfo().getSourceUrl()).isEmpty()) {
                noErrors = false;
                break;
            }
        }
        if (currentOpenedModule || noErrors) {
            testUnitsResults = model.runTest(testSuite, currentOpenedModule);
        } else {
            testUnitsResults = new TestUnitsResults(testSuite);
            testUnitsResults.setTestedRulesHaveErrors(true);
        }
        return testUnitsResults;
    }
}
