package org.openl.studio.projects.service.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.CompiledOpenClass;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.studio.projects.service.AbstractMethodExecutorService;
import org.openl.types.IOpenClass;

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
        CompiledOpenClass compiled = request.currentOpenedModule()
                ? request.projectModel().getOpenedModuleCompiledOpenClass()
                : request.projectModel().getCompiledOpenClass();
        IOpenClass openClass = compiled.getOpenClassWithErrors();
        ClassLoader classLoader = compiled.getClassLoader();

        TestSuite testSuite = buildTestSuite(request);

        TraceDebugger debugger = new TraceDebugger(request.listener());
        debugger.setBreakpoints(request.breakpoints());
        debugger.setWatches(request.watches());
        DebugSession session = new DebugSession(request.projectId(), request.tableId(), debugger, classLoader);

        debugger.start("trace-debug-" + request.tableId(), classLoader, request.stopAtEntry(), request.profiling(),
                () -> testSuite.invokeSequentially(openClass, 1));
        return session;
    }

    private TestSuite buildTestSuite(TraceDebugStartRequest request) {
        if (request.method() instanceof TestSuiteMethod testSuiteMethod) {
            return request.testRanges() == null
                    ? new TestSuite(testSuiteMethod)
                    : new TestSuite(testSuiteMethod, testSuiteMethod.getIndices(request.testRanges()));
        }
        var parsed = inputParserService.parseInput(request.inputJson(), request.method(), request.objectMapper());
        var resolvedMethod = resolveMethod(request.projectModel(), request.table(),
                request.currentOpenedModule(), parsed.runtimeContext());
        var db = getDb(request.projectModel(), request.currentOpenedModule());
        return new TestSuite(new TestDescription(resolvedMethod, parsed.runtimeContext(), parsed.params(), db));
    }
}
