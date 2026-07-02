package org.openl.rules.webstudio.web.trace.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Maintains the live execution stack on the worker thread and suspends at step and breakpoint points.
 *
 * <p>The stack and all callbacks run on the single worker thread, so the deque needs no
 * synchronization. When execution suspends, an immutable snapshot of the stack is published for the
 * controller thread to read; while suspended the worker mutates nothing, so the snapshot's frames and
 * their live arguments stay stable.
 */
final class DebugHookImpl implements DebugHook {

    private final SourceClassifier classifier;
    private final StepController stepController;
    private final DebugChannel channel;
    private final DebugListener listener;

    private final Deque<DebugFrame> stack = new ArrayDeque<>();
    private final AtomicReference<List<DebugFrame>> published = new AtomicReference<>(List.of());
    private @Nullable Throwable brokenException;
    /** Retain the structure of returned sub-calls so the executed call tree can be shown. Set before the worker runs. */
    private boolean profiling;
    /** Total time the worker spent parked at suspend points, subtracted from frame durations so think time is excluded. */
    private long parkedNanos;
    /** The whole executed tree, kept when the root frame returns so it outlives the empty stack on completion. */
    private @Nullable CallNode completedTree;
    /** A dispatcher currently choosing a version; the version it selects becomes the next frame, badged with it. */
    private @Nullable OpenMethodDispatcher pendingDispatch;
    /** The version the pending dispatcher selected (from its {@code rule} put), used to flag the chosen candidate. */
    private @Nullable IOpenMethod pendingChosen;

    DebugHookImpl(SourceClassifier classifier, StepController stepController, DebugChannel channel,
                  DebugListener listener) {
        this.classifier = classifier;
        this.stepController = stepController;
        this.channel = channel;
        this.listener = listener;
    }

    void setProfiling(boolean profiling) {
        this.profiling = profiling;
    }

    @Override
    public <T, E extends IRuntimeEnv, R> R bracketInvoke(Invokable<? super T, E> executor,
                                                         T target,
                                                         Object[] params,
                                                         E env,
                                                         Object source) {
        SourceClassifier.FrameDescriptor descriptor = classifier.describeFrame(source);
        if (descriptor != null) {
            return invokeFrame(descriptor, executor, target, params, env, source);
        }
        if (source instanceof OpenMethodDispatcher dispatcher) {
            // The dispatcher itself is transparent (no frame), but the version it selects — the next frame — is
            // badged with the choice, so run it while remembering which dispatcher is choosing.
            OpenMethodDispatcher previousDispatch = pendingDispatch;
            IOpenMethod previousChosen = pendingChosen;
            pendingDispatch = dispatcher;
            pendingChosen = null;
            try {
                return executor.invoke(target, params, env);
            } finally {
                pendingDispatch = previousDispatch;
                pendingChosen = previousChosen;
            }
        }
        DebugFrame top = stack.peek();
        CurrentLocation location = classifier.describeSubStep(executor, env, top == null ? null : top.getSource());
        if (location == null || top == null) {
            return executor.invoke(target, params, env);
        }
        // A step can start while another step of the same frame is still running: a formula computes the
        // cell it references before using it. Remember the enclosing step, so it can be restored when this
        // one completes — a table called from the rest of the enclosing formula belongs to it, not to the
        // step computed on the way.
        CurrentLocation enclosing = top.getLocation();
        Object enclosingStep = top.getCurrentStep();
        // Mark the current line, suspend if requested, then run the step and record its value so a later
        // suspension can show the results of already-executed steps.
        top.setCurrentStep(executor);
        top.setLocation(location);
        handleEvent(DebugEvent.LOCATION, top.getDepth(), top.getUri(), location, top.getName());
        // Time the step around its own execution, excluding parked time, so an inefficient step that makes no
        // sub-call is still tracked. Captured after the location suspend so the user's think time is not counted.
        long stepEnter = System.nanoTime();
        long parkedAtStepEnter = parkedNanos;
        // On failure the location intentionally stays on the failing step, so the exception break shows it.
        R result = executor.invoke(target, params, env);
        top.recordExecutedStep(executor, stepRef(location), location.label(), result,
                elapsed(stepEnter, parkedAtStepEnter));
        if (profiling && enclosing != null) {
            // Executed inside another step's formula: leave a reference there, like the legacy nested leaf.
            top.recordExecutedChild(stepRef(enclosing),
                    CallNode.referenceTo(top.getUri(), stepRef(location), location.label()));
        }
        top.setLocation(enclosing);
        top.setCurrentStep(enclosingStep);
        return result;
    }

    @Override
    public boolean onResolveNode(Object executor) {
        DebugFrame top = stack.peek();
        if (top == null) {
            return false;
        }
        DebugFrame.ExecutedStep original = top.executedStepFor(executor);
        if (original == null) {
            return false;
        }
        // A formula re-read a step that already executed: leave a reference to it, like the legacy
        // ref-to-node, but only under another step — a top-level re-read adds nothing to the tree.
        CurrentLocation location = top.getLocation();
        if (profiling && location != null && !stepRef(location).equals(original.ref())) {
            top.recordExecutedChild(stepRef(location),
                    CallNode.referenceTo(top.getUri(), original.ref(), original.label()));
        }
        return true;
    }

    private static String stepRef(CurrentLocation location) {
        if (location.ref() != null) {
            return location.ref();
        }
        return location.label() != null ? location.label() : location.kind();
    }

    /** Wall time since the frame entered, minus the time spent parked at suspend points: real execution time. */
    private long elapsed(long enterNanos, long parkedAtEnter) {
        return Math.max(0, System.nanoTime() - enterNanos - (parkedNanos - parkedAtEnter));
    }

    private <T, E extends IRuntimeEnv, R> R invokeFrame(SourceClassifier.FrameDescriptor descriptor,
                                                        Invokable<? super T, E> executor,
                                                        T target,
                                                        Object[] params,
                                                        E env,
                                                        Object source) {
        // The caller and the cell it is on are captured now, before the new frame is pushed, so the
        // returning frame can be attached to the exact step that made the call.
        DebugFrame parent = stack.peek();
        String callerRef = parent == null || parent.getLocation() == null ? null : stepRef(parent.getLocation());
        long enterNanos = System.nanoTime();
        long parkedAtEnter = parkedNanos;
        int depth = stack.size() + 1;
        DebugFrame frame = new DebugFrame(descriptor, source, target, params,
                env == null ? null : env.getContext(), depth);
        if (pendingDispatch != null) {
            // This frame is the version the pending dispatcher chose; badge it and consume the dispatch so only
            // the immediate child carries it.
            frame.setDispatch(DispatchInfo.of(pendingDispatch, pendingChosen));
            pendingDispatch = null;
            pendingChosen = null;
        }
        stack.push(frame);
        try {
            handleEvent(DebugEvent.ENTER, depth, descriptor.uri(), null, descriptor.name());
            R result = executor.invoke(target, params, env);
            frame.completeWith(result);
            // Time the frame the moment it finishes, before Step Out can suspend at its exit, so a completed
            // frame already on the stack carries its timing.
            frame.setDurationNanos(elapsed(enterNanos, parkedAtEnter));
            handleEvent(DebugEvent.EXIT, depth, descriptor.uri(), null, descriptor.name());
            return result;
        } catch (DebugTerminationError e) {
            throw e;
        } catch (Throwable ex) {
            frame.failWith(ex);
            frame.setDurationNanos(elapsed(enterNanos, parkedAtEnter));
            breakOnException(depth, ex);
            throw ex;
        } finally {
            stack.pop();
            // Profiling keeps the returned frame's structure (no values) so the executed call tree survives the pop.
            // A returned root frame has no parent to hold it, so it is kept as the completed tree instead. A frame
            // unwound by a terminate neither completed nor failed, so it is skipped — no misleading zero-time tree.
            if (profiling && (frame.isCompleted() || frame.getError() != null)) {
                CallNode node = frame.toCallNode();
                if (parent != null) {
                    parent.recordExecutedChild(callerRef, node);
                } else {
                    completedTree = node;
                }
            }
        }
    }

    @Override
    public void onPut(Object source, String id, Object[] args) {
        if (pendingDispatch == source && "rule".equals(id) && args.length > 0 && args[0] instanceof IOpenMethod chosen) {
            pendingChosen = chosen;
            return;
        }
        DebugFrame top = stack.peek();
        if (top == null) {
            return;
        }
        // Record decision-table condition results for the table view's green/red highlight. Condition
        // puts do not create step stops; the decision table's step stop is its fired rule.
        ConditionCheck check = classifier.describeCondition(id, args);
        if (check != null) {
            top.recordConditionCheck(check);
        }
    }

    private void handleEvent(DebugEvent event, int depth, String uri, @Nullable CurrentLocation location,
                             @Nullable String name) {
        if (channel.isTerminateRequested()) {
            throw new DebugTerminationError();
        }
        if (stepController.shouldSuspend(event, depth, uri, location, name)) {
            suspendAndAwait(depth);
        }
    }

    /**
     * Suspend at the frame where an exception surfaced so its state can be inspected before it
     * propagates. Each exception breaks once: as it unwinds through the outer frames, the same instance
     * is recognised and not re-broken.
     */
    private void breakOnException(int depth, Throwable ex) {
        if (ex == brokenException || channel.isTerminateRequested()) {
            return;
        }
        brokenException = ex;
        suspendAndAwait(depth);
    }

    /** Publish the current stack, park the worker as suspended, then re-arm stepping from the resuming command. */
    private void suspendAndAwait(int depth) {
        publishSnapshot();
        listener.onStatusChanged(DebugStatus.SUSPENDED);
        long parkStart = System.nanoTime();
        DebugCommand command = channel.awaitCommand();
        parkedNanos += System.nanoTime() - parkStart;
        stepController.arm(command, depth);
    }

    private void publishSnapshot() {
        List<DebugFrame> rootToTop = new ArrayList<>(stack.size());
        Iterator<DebugFrame> it = stack.descendingIterator();
        while (it.hasNext()) {
            rootToTop.add(it.next());
        }
        published.set(List.copyOf(rootToTop));
    }

    /** The most recently published stack, ordered from the root call to the current frame. */
    List<DebugFrame> snapshot() {
        return published.get();
    }

    /** The whole executed tree once the trace has finished, or {@code null} while it is still running. */
    @Nullable
    CallNode completedTree() {
        return completedTree;
    }

    /** The frame at the given stack index in the published snapshot, or {@code null} if out of range. */
    @Nullable
    DebugFrame frameAt(int index) {
        List<DebugFrame> current = published.get();
        return index >= 0 && index < current.size() ? current.get(index) : null;
    }
}
