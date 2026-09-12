package org.openl.studio.projects.service.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntToLongFunction;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.AbstractMethodExecutorService;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.tables.TableModules;

/**
 * Asynchronous implementation of {@link BenchmarkExecutorService}.
 * <p>
 * Executes measurements on the {@code testSuiteExecutor} thread pool to avoid
 * blocking HTTP request threads.
 * </p>
 */
@Validated
@Service
public class BenchmarkExecutorServiceImpl extends AbstractMethodExecutorService implements BenchmarkExecutorService {

    /**
     * How long one measurement lasts at the least. A measurement that ends sooner says more about the state
     * of the just-in-time compiler than about the rules.
     */
    private static final long MIN_NANOS = 3_000_000_000L;

    /** How many times longer one attempt may last than the attempt before it. */
    private static final double MAX_GROWTH = 200.0;

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<List<BenchmarkMeasurement>> benchmark(@NotNull ExecutionProgressListener listener,
                                                                   @NotNull ProjectModel projectModel,
                                                                   @NotNull IOpenLTable table,
                                                                   @Nullable String testRanges,
                                                                   Object @Nullable [] params,
                                                                   @Nullable IRulesRuntimeContext runtimeContext,
                                                                   boolean currentOpenedModule) {
        return executeWithLifecycle(listener, () -> {
            var method = resolveMethod(projectModel, table, currentOpenedModule, runtimeContext);
            var testSuite = method instanceof TestSuiteMethod testSuiteMethod
                    ? suiteOf(testSuiteMethod, testRanges)
                    : new TestSuite(new TestDescription(method, runtimeContext, params,
                            getDb(projectModel, currentOpenedModule)));
            return measure(projectModel, table, testSuite, currentOpenedModule);
        });
    }

    /**
     * The test suite to measure: every case of the test table, or the cases the ranges name.
     */
    private static TestSuite suiteOf(TestSuiteMethod testSuiteMethod, @Nullable String testRanges) {
        return testRanges == null
                ? new TestSuite(testSuiteMethod)
                : new TestSuite(testSuiteMethod, testSuiteMethod.getIndices(testRanges));
    }

    /**
     * Measures the suite, once as a whole or once per test case.
     *
     * <p>A test table whose every case is asked for is measured as a whole, the way it runs. A suite of chosen
     * cases is measured case by case, so that each of them is reported on its own.
     */
    private static List<BenchmarkMeasurement> measure(ProjectModel projectModel,
                                                      IOpenLTable table,
                                                      TestSuite testSuite,
                                                      boolean currentOpenedModule) throws InterruptedException {
        var openClass = compiledOpenClass(projectModel, currentOpenedModule).getOpenClassWithErrors();
        var measured = new Measured(TableUtils.makeTableId(testSuite.getUri()),
                moduleOf(projectModel, testSuite.getUri()),
                nameOf(testSuite, table),
                testSuite.getTestSuiteMethod() != null,
                isRunTable(testSuite));

        if (coversWholeTable(testSuite)) {
            return List.of(measureRepeatedly(measured,
                    testSuite.getNumberOfTests(),
                    List.of(),
                    runs -> testSuite.invokeSequentially(openClass, runs).getExecutionTime()));
        }

        var measurements = new ArrayList<BenchmarkMeasurement>();
        for (var testCase = 0; testCase < testSuite.getNumberOfTests(); testCase++) {
            var index = testCase;
            measurements.add(measureRepeatedly(measured,
                    1,
                    parametersOf(testSuite.getTest(index)),
                    runs -> testSuite.executeTest(openClass, index, runs).getExecutionTime()));
        }
        return measurements;
    }

    /**
     * Runs the execution until it lasts long enough, raising the number of runs as long as it ends too soon.
     *
     * <p>The first attempt runs once. Every attempt that ends before the measurement is meaningful is
     * repeated with as many runs as that time suggests, so that a fast table is measured over many runs and a
     * slow one over few.
     *
     * <p>Rules that throw stop at the first run, so no number of runs ever takes any time. Such a table
     * cannot be measured and is reported as an error instead of a meaningless row.
     */
    static BenchmarkMeasurement measureRepeatedly(Measured measured,
                                                  int testCases,
                                                  List<ParameterWithValueDeclaration> parameters,
                                                  IntToLongFunction execution) throws InterruptedException {
        var runs = 1;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Benchmark of " + measured.name() + " was cancelled");
            }
            var executionTime = execution.applyAsLong(runs);
            if (executionTime > MIN_NANOS) {
                return new BenchmarkMeasurement(UUID.randomUUID().toString(),
                        measured.tableId(),
                        measured.module(),
                        measured.name(),
                        measured.testTable(),
                        measured.runTable(),
                        testCases,
                        runs,
                        executionTime,
                        parameters);
            }
            if (runs == Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "%s cannot be measured: every run ends at once, which normally means the rules end with an error."
                                .formatted(measured.name()));
            }
            // A run that took no measurable time at all grows by the largest step allowed.
            var growth = Math.min(MAX_GROWTH, 1.1 * MIN_NANOS / executionTime);
            runs = Math.max(runs + 1, (int) (runs * growth));
        }
    }

    /** What every measurement of one benchmark says about the table it was taken on. */
    record Measured(String tableId,
                    @Nullable String module,
                    String name,
                    boolean testTable,
                    @Nullable Boolean runTable) {
    }

    /** The module the measured table is written in, so the results can send a reader to it. */
    private static @Nullable String moduleOf(ProjectModel projectModel, String tableUri) {
        var opened = projectModel.getModuleInfo();
        return TableModules.of(opened == null ? null : opened.getProject()).moduleOf(tableUri);
    }

    /** The input of one test case, under the names the table author wrote for them. */
    private static List<ParameterWithValueDeclaration> parametersOf(TestDescription testCase) {
        return Arrays.stream(testCase.getExecutionParams()).toList();
    }

    /** Whether the suite covers every case of its test table, and so is measured the way the table runs. */
    private static boolean coversWholeTable(TestSuite testSuite) {
        var testSuiteMethod = testSuite.getTestSuiteMethod();
        return testSuiteMethod != null && testSuite.getNumberOfTests() == testSuiteMethod.getNumberOfTestsCases();
    }

    /** Whether the measured table is a Run table, which states no expected values. */
    private static @Nullable Boolean isRunTable(TestSuite testSuite) {
        var testSuiteMethod = testSuite.getTestSuiteMethod();
        return testSuiteMethod != null && testSuiteMethod.isRunMethod() ? Boolean.TRUE : null;
    }

    /**
     * The name the measured table is reported under: the display name of the test table, or of the table the
     * benchmark was started from when it is not a test table.
     */
    private static String nameOf(TestSuite testSuite, IOpenLTable table) {
        var testSuiteMethod = testSuite.getTestSuiteMethod();
        return testSuiteMethod == null
                ? table.getDisplayName()
                : TableSyntaxNodeUtils.getTestName(testSuiteMethod);
    }
}
