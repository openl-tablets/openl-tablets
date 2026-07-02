package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.trace.debug.SourceClassifier.FrameDescriptor;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * Exercises the engine end-to-end through the real {@link Tracer} dispatch on a worker thread, using
 * synthetic tables and cells so the test needs no compiled OpenL module.
 */
class TraceDebuggerIntegrationTest {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final long TIMEOUT = 5_000;

    /** A synthetic table: entering it is a frame, its body runs nested steps. */
    private static final class FakeTable implements Invokable<Object, IRuntimeEnv> {
        private final String uri;
        private final List<Consumer<IRuntimeEnv>> body = new ArrayList<>();

        FakeTable(String uri) {
            this.uri = uri;
        }

        FakeTable cell(int row, int col) {
            body.add(env -> Tracer.invoke(new FakeCell(row, col), null, NO_PARAMS, env, new FakeCell(row, col)));
            return this;
        }

        FakeTable call(FakeTable child) {
            body.add(env -> Tracer.invoke(child, null, NO_PARAMS, env, child));
            return this;
        }

        FakeTable boom() {
            body.add(env -> {
                throw new IllegalStateException("boom");
            });
            return this;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            body.forEach(step -> step.accept(env));
            return uri + ":done";
        }
    }

    /** A synthetic sub-step: a spreadsheet-like cell. */
    private record FakeCell(int row, int col) implements Invokable<Object, IRuntimeEnv> {
        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return "R" + row + "C" + col;
        }
    }

    private static final SourceClassifier CLASSIFIER = new SourceClassifier() {
        @Override
        public FrameDescriptor describeFrame(Object source) {
            return source instanceof FakeTable t ? new FrameDescriptor(FrameKind.METHOD, t.uri, t.uri) : null;
        }

        @Override
        public CurrentLocation describeSubStep(Object executor, IRuntimeEnv env, Object frameSource) {
            return executor instanceof FakeCell c ? CurrentLocation.cell(c.row(), c.col()) : null;
        }
    };

    private static DebugBody program() {
        FakeTable t1 = new FakeTable("T1").cell(1, 0);
        FakeTable t0 = new FakeTable("T0").cell(0, 0).call(t1).cell(0, 1);
        return () -> Tracer.invoke(t0, null, NO_PARAMS, new SimpleRuntimeEnv(), t0);
    }

    private static List<String> uris(TraceDebugger debugger) {
        return debugger.stack().stream().map(DebugFrame::getUri).toList();
    }

    private static String topRef(TraceDebugger debugger) {
        List<DebugFrame> stack = debugger.stack();
        CurrentLocation location = stack.get(stack.size() - 1).getLocation();
        assertNotNull(location, "expected a current location");
        return location.ref();
    }

    @Test
    void stepIntoOverOutWalkTheStack() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, true, program());

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(List.of("T0"), uris(debugger));

        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(List.of("T0"), uris(debugger));
        assertEquals("R0C0", topRef(debugger));

        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));

        // Step out of T1: run it to completion and stop at T1's own exit, where its result is on the stack.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));
        assertTrue(debugger.stack().get(1).isCompleted(), "the returning frame is on the stack with its result");

        // A further step continues in the caller, on T0's next line.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(List.of("T0"), uris(debugger));
        assertEquals("R0C1", topRef(debugger));

        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    @Test
    void breakOnExceptionSuspendsAtThrowingFrame() {
        FakeTable t1 = new FakeTable("T1").boom();
        FakeTable t0 = new FakeTable("T0").call(t1);
        DebugBody program = () -> Tracer.invoke(t0, null, NO_PARAMS, new SimpleRuntimeEnv(), t0);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        // Run with no breakpoints: without break-on-exception this would just fail; instead it suspends.
        debugger.start("test-worker", null, false, program);

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));
        assertNotNull(debugger.stack().get(1).getError(), "the throwing frame carries its error");

        // Resuming lets the exception propagate; the session ends in error (it does not re-break per frame).
        assertEquals(DebugStatus.ERROR, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    @Test
    void stepOverRunsThroughCalleeTables() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, true, program());

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        // At T0 entry, step over to the first cell.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OVER, TIMEOUT));
        assertEquals("R0C0", topRef(debugger));

        // Step over the nested T1 call: land on T0's next cell, never inside T1.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OVER, TIMEOUT));
        assertEquals(List.of("T0"), uris(debugger));
        assertEquals("R0C1", topRef(debugger));
    }

    @Test
    void resumeStopsAtBreakpoint() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.setBreakpoints(Set.of("T1"));
        debugger.start("test-worker", null, false, program());

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));

        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    @Test
    void runToCompletionWithoutBreakpoints() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, false, program());
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(TIMEOUT));
    }

    @Test
    void recordsExecutedStepValuesAsSubStepsRun() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, true, program());
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));

        // At the first cell, suspended before it computes: nothing recorded yet.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertTrue(debugger.stack().get(0).getExecutedSteps().isEmpty());

        // Next step computes the first cell and records its value, then stops at the nested call.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        DebugFrame root = debugger.stack().get(0);
        assertFalse(root.getExecutedSteps().isEmpty(), "the executed cell must be recorded");
        DebugFrame.ExecutedStep first = root.getExecutedSteps().get(0);
        assertEquals("R0C0", first.ref());
        assertEquals("R0C0", first.value());

        debugger.terminate(TIMEOUT);
    }

    @Test
    void terminateCancelsSuspendedSession() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, true, program());
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));

        debugger.terminate(TIMEOUT);
        assertTrue(debugger.status().isTerminal());
        assertEquals(DebugStatus.TERMINATED, debugger.status());
    }

    @Test
    void terminateDoesNotBlockOnAnUninterruptibleWorker() throws InterruptedException {
        var inLoop = new CountDownLatch(1);
        var release = new AtomicBoolean(false);
        // A rule that never reaches a safepoint and ignores interruption (a tight loop).
        DebugBody spinning = () -> {
            inLoop.countDown();
            while (!release.get()) {
                Thread.onSpinWait();
            }
        };

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        try {
            debugger.start("stuck-worker", null, false, spinning);
            assertTrue(inLoop.await(TIMEOUT, TimeUnit.MILLISECONDS), "the worker should reach its loop");

            long startNanos = System.nanoTime();
            debugger.terminate(100);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

            // Terminate joins briefly then returns; it must not block on the runaway worker.
            assertTrue(elapsedMillis < TIMEOUT, "terminate must return promptly, not block on the worker");
            assertFalse(debugger.status().isTerminal(), "the abandoned worker is still running");
        } finally {
            release.set(true);
        }
    }
}
