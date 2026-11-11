package org.openl.studio.projects.service.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.util.Arrays;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.types.IOpenMethod;

/**
 * Runs the tests of a project on the {@code testSuiteExecutor} pool.
 *
 * <p>A run tests the project as it was compiled when the run started, and holds nothing the editor waits for: the
 * project can be read and written while the tests go on.
 *
 * <p>A run stops before the next case once its thread is interrupted, or once the project is compiled again, and
 * reports itself {@link TestExecutionStatus#INTERRUPTED interrupted}. The results of the cases that ran are kept.
 */
@Service
public class TestsExecutorServiceImpl implements TestsExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<TestRun> runAll(ProjectTestsExecutionProgressListener listener,
                                             ProjectModel projectModel,
                                             TableModules modules,
                                             boolean currentOpenedModule) {
        var run = new Compiled(projectModel, currentOpenedModule);
        var testMethods = currentOpenedModule ? projectModel.getOpenedModuleTestMethods() : projectModel.getAllTestMethods();
        var notRun = notRunTables(projectModel, testMethods, currentOpenedModule);
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        runAllTests(listener, executionResult, run, testMethods);
        return CompletableFuture.completedFuture(new TestRun(executionResult, notRun, modules));
    }

    private static List<String> notRunTables(ProjectModel model,
                                             TestSuiteMethod @Nullable [] ran,
                                             boolean currentOpenedModule) {
        var ranUris = ran == null ? Set.<String>of()
                : Stream.of(ran).map(TestSuiteMethod::getSourceUrl).collect(Collectors.toSet());
        var tables = currentOpenedModule ? Stream.of(model.getTableSyntaxNodes())
                : model.getAllTableSyntaxNodes().stream();
        return tables
                .filter(node -> node.getNodeType() == XlsNodeTypes.XLS_TEST_METHOD && !ranUris.contains(node.getUri()))
                .filter(node -> !model.getErrorsByUri(node.getUri()).isEmpty())
                .map(TableSyntaxNodeUtils::getTableName)
                .distinct()
                .toList();
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<TestRun> runAllForTable(ProjectTestsExecutionProgressListener listener,
                                                     ProjectModel projectModel,
                                                     TableModules modules,
                                                     IOpenLTable table,
                                                     boolean currentOpenedModule) {
        var run = new Compiled(projectModel, currentOpenedModule);
        var uri = table.getUri();
        IOpenMethod method = currentOpenedModule ? projectModel.getOpenedModuleMethod(uri) : projectModel.getMethod(uri);
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        if (method instanceof TestSuiteMethod) {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(new TestRun(executionResult, modules));
        }
        var testMethods = projectModel.getTestMethods(uri, currentOpenedModule);
        runAllTests(listener, executionResult, run, testMethods);
        return CompletableFuture.completedFuture(new TestRun(executionResult, modules));
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<TestRun> runSingle(ProjectTestsExecutionProgressListener listener,
                                                ProjectModel projectModel,
                                                TableModules modules,
                                                IOpenLTable table,
                                                String testRanges,
                                                boolean currentOpenedModule) {
        var run = new Compiled(projectModel, currentOpenedModule);
        var uri = table.getUri();
        IOpenMethod method = currentOpenedModule ? projectModel.getOpenedModuleMethod(uri) : projectModel.getMethod(uri);
        var executionResult = new ArrayList<TestUnitsResults>();
        listener.onStatusChanged(TestExecutionStatus.STARTED);
        if (method instanceof TestSuiteMethod testSuiteMethod) {
            // Every case of the test table, or only the cases the ranges name.
            var testSuite = testRanges == null
                    ? new TestSuite(testSuiteMethod)
                    : new TestSuite(testSuiteMethod, testSuiteMethod.getIndices(testRanges));
            var unitsResult = run.test(testSuite);
            if (unitsResult != null) {
                listener.onTestUnitExecuted(unitsResult);
                executionResult.add(unitsResult);
            }
        }
        listener.onStatusChanged(run.stopped() ? TestExecutionStatus.INTERRUPTED : TestExecutionStatus.COMPLETED);
        return CompletableFuture.completedFuture(new TestRun(executionResult, modules));
    }

    private void runAllTests(ProjectTestsExecutionProgressListener listener,
                             List<TestUnitsResults> executionResult,
                             Compiled run,
                             TestSuiteMethod[] tests) {
        if (Arrays.isEmpty(tests)) {
            listener.onStatusChanged(TestExecutionStatus.COMPLETED);
            return;
        }

        for (TestSuiteMethod testSuiteMethod : tests) {
            var testUnitsResults = runSingleTest(run, testSuiteMethod);
            if (testUnitsResults == null) {
                break;
            }
            executionResult.add(testUnitsResults);
            listener.onTestUnitExecuted(testUnitsResults);
        }
        listener.onStatusChanged(run.stopped() ? TestExecutionStatus.INTERRUPTED : TestExecutionStatus.COMPLETED);
    }

    /**
     * Runs one test table.
     *
     * @return the results of its cases, or {@code null} when the run was stopped before the table
     */
    private static @Nullable TestUnitsResults runSingleTest(Compiled run, TestSuiteMethod testSuiteMethod) {
        if (run.stopped()) {
            return null;
        }
        var testedMethod = testSuiteMethod.getTestedMethod();
        var testSuite = new TestSuite(testSuiteMethod);
        Collection<IOpenMethod> methods = testedMethod instanceof OpenMethodDispatcher dispatcher
                ? dispatcher.getCandidates()
                : Set.of(testedMethod);
        var noErrors = true;
        for (IOpenMethod method : methods) {
            if (!run.model().getErrorsByUri(method.getInfo().getSourceUrl()).isEmpty()) {
                noErrors = false;
                break;
            }
        }
        if (run.currentOpenedModule() || noErrors) {
            return run.test(testSuite);
        }
        var testUnitsResults = new TestUnitsResults(testSuite);
        testUnitsResults.setTestedRulesHaveErrors(true);
        return testUnitsResults;
    }

    /**
     * The project, or the module that is open, as it was compiled when a run started.
     *
     * @param model               the project the run runs in
     * @param currentOpenedModule whether the run is of the module that is open rather than of the whole project
     * @param compiled            what the run tests
     * @param rerun               runs a case of the run again for the values the run gives back
     */
    private record Compiled(ProjectModel model,
                            boolean currentOpenedModule,
                            CompiledOpenClass compiled,
                            TestsRerun rerun) {

        Compiled(ProjectModel model, boolean currentOpenedModule) {
            this(model, currentOpenedModule, compiledOf(model, currentOpenedModule));
        }

        private Compiled(ProjectModel model, boolean currentOpenedModule, CompiledOpenClass compiled) {
            this(model, currentOpenedModule, compiled, TestsRerun.of(model, currentOpenedModule, compiled));
        }

        /**
         * Runs a test table, or the cases of it a suite names.
         *
         * @return the results of the cases that ran, or {@code null} when the project was compiled again
         */
        @Nullable TestUnitsResults test(TestSuite testSuite) {
            var results = model.runTest(testSuite, currentOpenedModule, compiled);
            return results == null ? null : RetainedTestUnit.retain(results, rerun);
        }

        /** Whether the run is asked to stop, or tests what the project no longer is, or what was let go of. */
        boolean stopped() {
            return Thread.currentThread().isInterrupted() || !model.isCompiledAsRead(compiled, currentOpenedModule);
        }

        private static CompiledOpenClass compiledOf(ProjectModel model, boolean currentOpenedModule) {
            return currentOpenedModule ? model.getOpenedModuleCompiledOpenClass() : model.getCompiledOpenClass();
        }
    }
}
