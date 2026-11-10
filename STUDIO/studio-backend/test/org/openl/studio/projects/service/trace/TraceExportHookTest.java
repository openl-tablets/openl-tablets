package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Trace into File of a rule that calls itself until the stack overflows.
 *
 * <p>Fixture {@code recursionProject.xlsx}: {@code Recurse(Integer n)} computes its only step as
 * {@code Recurse(n + 1)}, so every call fails with {@code StackOverflowError}.
 */
class TraceExportHookTest {

    private static final String SRC = "test/rules/trace-debug/recursionProject.xlsx";
    private static final String FAILED_CALL = "TRACE: SpreadSheet SpreadsheetResultRecurse Recurse(Integer n) = ERROR";

    @Test
    void writesEveryCallOfAStackOverflowAsFailed() throws Exception {
        var hook = new TraceExportHook(new DefaultSourceClassifier(), false, 200_000);

        assertInstanceOf(StackOverflowError.class, runToTheOverflow(hook));

        // Each call is a line with the line of its step under it, one level deeper.
        var calls = callLines(write(hook));
        assertFalse(calls.isEmpty());
        assertEquals(List.of(), calls.stream().filter(line -> !FAILED_CALL.equals(line)).toList(),
                "the deepest calls, which failed where the stack ran out, read as failed too");
    }

    /** Runs the rule with the hook on a small stack of its own, which keeps the recursion short. */
    private static Throwable runToTheOverflow(TraceExportHook hook) throws InterruptedException {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var recurse = module.getMethod("Recurse", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});
        var failure = new AtomicReference<Throwable>();
        var worker = new Thread(null, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(new DebugTracer(hook));
            try {
                recurse.invoke(module.newInstance(env), new Object[]{1}, env);
            } catch (RuntimeException | Error e) {
                failure.set(e);
            }
        }, "export-overflow", 512 * 1024);
        worker.start();
        worker.join();
        return failure.get();
    }

    private static String write(TraceExportHook hook) throws IOException {
        var out = new StringWriter();
        hook.writeTo(out);
        return out.toString();
    }

    /** The {@code TRACE:} lines of the calls: those at an even nesting level, the steps being at the odd ones. */
    private static List<String> callLines(String trace) {
        return trace.lines()
                .filter(line -> line.strip().startsWith("TRACE:"))
                .filter(line -> (line.length() - line.stripLeading().length()) % 2 == 0)
                .map(String::strip)
                .toList();
    }
}
