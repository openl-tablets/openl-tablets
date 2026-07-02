package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            body.add(env -> Tracer.invoke(new FakeCell(row, col, null), null, NO_PARAMS, env,
                    new FakeCell(row, col, null)));
            return this;
        }

        /** A cell whose formula calls another table — the call happens inside the cell, as in a spreadsheet. */
        FakeTable cellCalling(int row, int col, FakeTable child) {
            body.add(env -> Tracer.invoke(new FakeCell(row, col, child), null, NO_PARAMS, env,
                    new FakeCell(row, col, child)));
            return this;
        }

        /** A direct table-to-table call with no step in between, as in a method table body. */
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

        FakeTable rule(String name) {
            body.add(env -> Tracer.invoke(new FakeRule(name), null, NO_PARAMS, env, new FakeRule(name)));
            return this;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            body.forEach(step -> step.accept(env));
            return uri + ":done";
        }
    }

    /** A synthetic sub-step: a spreadsheet-like cell, optionally calling a table from its formula. */
    private record FakeCell(int row, int col, FakeTable calls) implements Invokable<Object, IRuntimeEnv> {
        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (calls != null) {
                Tracer.invoke(calls, null, NO_PARAMS, env, calls);
            }
            return "R" + row + "C" + col;
        }
    }

    /** A synthetic sub-step: a fired decision-table rule. */
    private record FakeRule(String name) implements Invokable<Object, IRuntimeEnv> {
        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return name + ":fired";
        }
    }

    private static final SourceClassifier CLASSIFIER = new SourceClassifier() {
        @Override
        public FrameDescriptor describeFrame(Object source) {
            return source instanceof FakeTable t ? new FrameDescriptor(FrameKind.METHOD, t.uri, t.uri) : null;
        }

        @Override
        public CurrentLocation describeSubStep(Object executor, IRuntimeEnv env, Object frameSource) {
            return switch (executor) {
                case FakeCell c -> CurrentLocation.cell(c.row(), c.col());
                case FakeRule r -> CurrentLocation.dtRule(List.of(r.name()));
                case null, default -> null;
            };
        }
    };

    private static DebugBody program() {
        FakeTable t1 = new FakeTable("T1").cell(1, 0);
        FakeTable t0 = new FakeTable("T0").cellCalling(0, 0, t1).cell(0, 1);
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
    void capturesAWatchedCellOnEveryExecutionOfItsTable() {
        // T0 calls T1 twice; each T1 execution computes the watched cell R0C0.
        FakeTable t1 = new FakeTable("T1").cell(0, 0);
        FakeTable t0 = new FakeTable("T0").call(t1).call(t1);
        DebugBody body = () -> Tracer.invoke(t0, null, NO_PARAMS, new SimpleRuntimeEnv(), t0);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.setWatches(Set.of("R0C0"));
        debugger.start("watch-test", null, true, body);
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));

        List<WatchCapture> captures = debugger.watchCaptures();
        assertEquals(2, captures.size(), "the watched cell computed once per T1 execution");
        assertEquals(List.of(0, 1), captures.stream().map(WatchCapture::instance).toList(),
                "the two executions are numbered 0 and 1");
        assertEquals("T1", captures.get(0).table());
        assertEquals("T1#R0C0", captures.get(0).ref());
        assertEquals("R0C0", captures.get(0).value(), "the fake cell returns its own ref as the value");
        assertEquals(List.of("T0", "T1"), captures.get(0).path(), "the path runs from the root to the owning frame");
        assertFalse(debugger.isWatchTruncated());
    }

    @Test
    void anInstanceIndexedBreakpointStopsOnThatExecutionOfARepeatedTable() {
        // T0 calls T1 twice; the breakpoint targets only T1's second execution (instance 1).
        FakeTable t1 = new FakeTable("T1").cell(0, 0);
        FakeTable t0 = new FakeTable("T0").call(t1).call(t1);
        DebugBody body = () -> Tracer.invoke(t0, null, NO_PARAMS, new SimpleRuntimeEnv(), t0);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.setBreakpoints(Set.of("T1#R0C0@1"));
        debugger.start("instance-bp", null, false, body);

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        DebugFrame top = debugger.stack().get(debugger.stack().size() - 1);
        assertEquals("T1", top.getName());
        assertEquals(1, top.getInvocationIndex(), "stopped on T1's second execution, not the first");
        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    @Test
    void stepOverToFrameEndSuspendsAtItsExitWithTheResult() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, true, program());
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));

        // Walk into T1's only cell.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));
        assertEquals("R1C0", topRef(debugger));

        // Stepping over T1's last line finishes the frame but stops at its own exit, not in the caller,
        // so the returned result can be rendered before the frame closes.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OVER, TIMEOUT));
        assertEquals(List.of("T0", "T1"), uris(debugger));
        DebugFrame returning = debugger.stack().get(1);
        assertTrue(returning.isCompleted(), "the finished frame stays on the stack with its result");
        assertEquals("T1:done", returning.getResult());

        // A further step then continues in the caller, on T0's next line.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OVER, TIMEOUT));
        assertEquals(List.of("T0"), uris(debugger));
        assertEquals("R0C1", topRef(debugger));

        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    /** Walk T0 → T1 → out, leaving T0 live after T1 returned; used by both profiling cases below. */
    private static void runUntilSubCallReturned(TraceDebugger debugger) {
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT)); // T0:R0C0
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT)); // enter T1
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, TIMEOUT));  // T1 completes
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT)); // back in T0, T1 popped
        assertEquals(List.of("T0"), uris(debugger));
    }

    @Test
    void profilingRetainsAReturnedSubCallUnderTheCallingCell() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("profiling-worker", null, true, true, program());
        try {
            runUntilSubCallReturned(debugger);
            DebugFrame t0 = debugger.stack().get(0);
            assertTrue(t0.getExecutedChildren().containsKey("R0C0"),
                    "the returned sub-call is retained under the cell that called it");
            CallNode t1 = t0.getExecutedChildren().get("R0C0").get(0);
            assertEquals("T1", t1.name());
            assertEquals(FrameKind.METHOD, t1.kind());
            assertEquals(List.of("R1C0"), t1.steps().stream().map(CallNode.Step::ref).toList());
        } finally {
            debugger.terminate(TIMEOUT);
        }
    }

    @Test
    void withoutProfilingReturnedSubCallsAreNotRetained() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("plain-worker", null, true, program());
        try {
            runUntilSubCallReturned(debugger);
            assertTrue(debugger.stack().get(0).getExecutedChildren().isEmpty(),
                    "off by default: a returned sub-call leaves no structure behind");
        } finally {
            debugger.terminate(TIMEOUT);
        }
    }

    @Test
    void profilingKeepsTheWholeTreeWithTimingsAfterCompletion() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        // No stop-at-entry, profiling on: it runs straight to completion, keeping the executed tree.
        debugger.start("completed-tree-worker", null, false, true, program());
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(TIMEOUT));

        CallNode tree = debugger.completedTree();
        assertNotNull(tree, "the whole executed tree outlives the empty stack on completion");
        assertEquals("T0", tree.name());
        CallNode.Step caller = tree.steps().stream()
                .filter(step -> step.ref().equals("R0C0")).findFirst().orElseThrow();
        assertEquals(List.of("T1"), caller.children().stream().map(CallNode::name).toList());
        assertTrue(tree.durationNanos() >= 0, "the root carries its measured execution time");
        assertFalse(debugger.isTreeTruncated(), "a small run stays well under the node cap");
    }

    @Test
    void withoutProfilingNoTreeIsKeptAfterCompletion() {
        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("plain-completion-worker", null, false, program());
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(TIMEOUT));
        assertNull(debugger.completedTree(), "off by default: nothing is kept after completion");
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
    void ruleFiredBreakpointSuspendsWhenARuleFires() {
        FakeTable dt = new FakeTable("DT").rule("R3");
        DebugBody program = () -> Tracer.invoke(dt, null, NO_PARAMS, new SimpleRuntimeEnv(), dt);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.setBreakpoints(Set.of("DT#" + CurrentLocation.RULE_FIRED_REF));
        // No stop-at-entry: only the rule-fired breakpoint should suspend it.
        debugger.start("test-worker", null, false, program);

        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        assertEquals(List.of("DT"), uris(debugger));
        CurrentLocation location = debugger.stack().get(0).getLocation();
        assertNotNull(location, "expected a current location");
        assertEquals("R3", location.label(), "suspended at the fired rule");

        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
    }

    @Test
    void ruleFiringDoesNotSuspendWithoutARuleFiredBreakpoint() {
        FakeTable dt = new FakeTable("DT").rule("R3");
        DebugBody program = () -> Tracer.invoke(dt, null, NO_PARAMS, new SimpleRuntimeEnv(), dt);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.start("test-worker", null, false, program);

        // With no breakpoint and no stop-at-entry, a fired rule is an ordinary safepoint and runs through.
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(TIMEOUT));
    }

    @Test
    void ruleBreakpointSuspendsOnlyOnTheNamedRule() {
        FakeTable dt = new FakeTable("DT").rule("R1").rule("R2");
        DebugBody program = () -> Tracer.invoke(dt, null, NO_PARAMS, new SimpleRuntimeEnv(), dt);

        TraceDebugger debugger = new TraceDebugger(CLASSIFIER);
        debugger.setBreakpoints(Set.of("DT#R2"));
        debugger.start("test-worker", null, false, program);

        // R1 fires first but is not the named rule, so execution runs on to R2.
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(TIMEOUT));
        CurrentLocation location = debugger.stack().get(0).getLocation();
        assertNotNull(location, "expected a current location");
        assertEquals("R2", location.label(), "stopped at R2, not R1");

        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, TIMEOUT));
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

        // The first cell's formula calls a table: at that call's entry the cell is still executing.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, TIMEOUT));
        assertTrue(debugger.stack().get(0).getExecutedSteps().isEmpty(),
                "a cell still running its formula is not executed yet");

        // Once the call returns and the cell completes, its value is recorded — visible at the next cell.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, TIMEOUT));
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
