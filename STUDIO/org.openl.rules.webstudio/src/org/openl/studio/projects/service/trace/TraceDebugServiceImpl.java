package org.openl.studio.projects.service.trace;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.CompiledOpenClass;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.studio.projects.service.AbstractMethodExecutorService;

/**
 * Builds a debug session and runs it on a dedicated worker thread.
 *
 * <p>The compiled class and classloader are captured once, then the test suite is invoked directly on
 * the worker via {@code invokeSequentially}. This never enters {@code ProjectModel.traceElement}, so the
 * project monitor is not held while execution is suspended.
 */
@Component
@RequiredArgsConstructor
public class TraceDebugServiceImpl extends AbstractMethodExecutorService implements TraceDebugService {

    private final TableInputParserService inputParserService;

    @Override
    public DebugSession startSession(TraceDebugStartRequest request) {
        var compiled = compiledOf(request);
        var openClass = compiled.getOpenClassWithErrors();
        var classLoader = compiled.getClassLoader();

        var testSuite = buildTestSuite(request);

        var debugger = new TraceDebugger(request.listener());
        debugger.setBreakpoints(request.breakpoints());
        debugger.setWatches(request.watches());
        // Build the export replay's suite lazily, only if "Trace into File" is actually used. Building it
        // eagerly held a full second copy of the parsed input for the whole session — large for a big request,
        // and wasted whenever the run is never exported. Building it on demand also gives a distinct suite, so
        // the export never re-enters the interactive suite while its worker is parked mid-run. Resolve the class
        // to run against at export time too, from the same model buildTestSuite resolves the method from, so an
        // in-place recompile between start and export cannot pair a fresh method with the stale start-time class.
        var session = new DebugSession(request.projectId(), request.tableId(), debugger, classLoader,
                tracer -> buildTestSuite(request).invokeSequentially(compiledOf(request).getOpenClassWithErrors(), 1, tracer),
                request.sessionId());

        debugger.start("trace-debug-" + request.tableId(), classLoader, request.stopAtEntry(), request.profiling(),
                () -> testSuite.invokeSequentially(openClass, 1, debugger.tracer()));
        return session;
    }

    private CompiledOpenClass compiledOf(TraceDebugStartRequest request) {
        return request.currentOpenedModule()
                ? request.projectModel().getOpenedModuleCompiledOpenClass()
                : request.projectModel().getCompiledOpenClass();
    }

    private TestSuite buildTestSuite(TraceDebugStartRequest request) {
        if (request.method() instanceof TestSuiteMethod testSuiteMethod) {
            // Trace exactly one test case: several cases just stack in the tree and rarely help, so a
            // requested range (or the whole suite) collapses to its first case.
            return new TestSuite(testSuiteMethod, firstTestIndex(testSuiteMethod, request.testRanges()));
        }
        var parsed = inputParserService.parseInput(request.inputJson(), request.method(), request.objectMapper());
        var resolvedMethod = resolveMethod(request.projectModel(), request.table(),
                request.currentOpenedModule(), parsed.runtimeContext());
        var db = getDb(request.projectModel(), request.currentOpenedModule());
        return new TestSuite(new TestDescription(resolvedMethod, parsed.runtimeContext(), parsed.params(), db));
    }

    /** The single test case to trace: the first of the requested range, or the first case for the whole suite. */
    static int firstTestIndex(TestSuiteMethod method, @Nullable String testRanges) {
        if (testRanges == null) {
            return 0;
        }
        var indices = method.getIndices(testRanges);
        return indices.length > 0 ? indices[0] : 0;
    }
}
