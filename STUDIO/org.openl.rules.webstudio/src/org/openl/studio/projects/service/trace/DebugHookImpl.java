package org.openl.studio.projects.service.trace;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.studio.projects.model.trace.DebugStatus;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.studio.projects.model.trace.TableProfile;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.util.StringPool;
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

    /** Upper bound on watch captures, so a watched cell in a huge loop cannot grow the session unbounded. */
    private static final int MAX_WATCH_CAPTURES = 50_000;

    /**
     * Default upper bound on retained call-tree nodes, so one huge profiled run cannot balloon the heap. Since
     * the tree is now serialized lazily (a level at a time), this is a memory bound only, not a response-size
     * one.
     */
    private static final int MAX_TREE_NODES = 5_000_000;

    /**
     * Upper bound on the nodes any single branch may retain in its subtree. It keeps the earliest, deepest
     * branch from consuming the whole {@link #MAX_TREE_NODES} budget and starving its later siblings, so every
     * branch stays browsable even when the run as a whole is truncated.
     */
    private static final int MAX_BRANCH_NODES = 1_000_000;

    private final Deque<DebugFrame> stack = new ArrayDeque<>();
    private final AtomicReference<List<DebugFrame>> published = new AtomicReference<>(List.of());
    private @Nullable Throwable brokenException;
    /** Retain the structure of returned sub-calls so the executed call tree can be shown. Set before the worker runs. */
    private boolean profiling;
    /** Cell names or refs whose value is captured on every execution of their table. May be updated mid-run. */
    private volatile Set<String> watches = Set.of();
    /** Captured watched values, appended on the worker thread as cells compute. */
    private final List<WatchCapture> captures = new ArrayList<>();
    /** Per-table invocation counter, so each execution of a table gets a stable instance number. */
    private final Map<String, Integer> invocationCounts = new HashMap<>();
    /** Set when the capture cap was hit, so the response can say the watch series is incomplete. */
    private volatile boolean watchTruncated;
    /** Total time the worker spent parked at suspend points, subtracted from frame durations so think time is excluded. */
    private long parkedNanos;
    /**
     * Monotonic running total of every step's wall-clock. A step reads it on entry and after its own execution:
     * the difference is the time spent in nested steps (referenced cells computed on the way), which is subtracted
     * from its own time so a cell that triggers another does not count that other cell twice.
     */
    private long nestedStepNanos;
    /** The whole executed tree, kept when the root frame returns so it outlives the empty stack on completion. */
    private final AtomicReference<@Nullable CallNode> completedTree = new AtomicReference<>();
    /** Running count of retained tree nodes and the flag set once the node cap stops the tree from growing. */
    private int recordedNodes;
    private volatile boolean treeTruncated;
    /** Effective node cap; defaults to {@link #MAX_TREE_NODES}, lowered by tests to exercise truncation. */
    private int maxTreeNodes = MAX_TREE_NODES;
    /** Per-table profiling stats, accumulated as frames complete — independent of the retained (capped) tree. */
    private final Map<String, TableAccumulator> tableStats = new LinkedHashMap<>();
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

    /** Watch a set of cells by name ({@code $...} label) or ref, capturing their value on every execution. */
    void setWatches(Set<String> watches) {
        this.watches = Set.copyOf(watches);
    }

    Set<String> getWatches() {
        return watches;
    }

    /** All watched-cell captures gathered so far. Read while the worker is parked or finished. */
    List<WatchCapture> watchCaptures() {
        return List.copyOf(captures);
    }

    boolean isWatchTruncated() {
        return watchTruncated;
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
        String ref = stepRef(location);
        handleEvent(DebugEvent.LOCATION, top.getDepth(), top.getUri(), location, top.getName(),
                top.getInvocationIndex());
        // Time the step around its own execution, excluding parked time, so an inefficient step that makes no
        // sub-call is still tracked. Captured after the location suspend so the user's think time is not counted.
        long stepEnter = System.nanoTime();
        long parkedAtStepEnter = parkedNanos;
        long nestedAtStepEnter = nestedStepNanos;
        // On failure the location intentionally stays on the failing step, so the exception break shows it.
        R result = executor.invoke(target, params, env);
        // A referenced cell computed on the way runs as a nested step of the same frame and is recorded on its
        // own; subtract that nested time so this step's own time does not count the referenced cell twice.
        long wall = elapsed(stepEnter, parkedAtStepEnter);
        top.recordExecutedStep(executor, ref, location.label(), result,
                Math.max(0, wall - (nestedStepNanos - nestedAtStepEnter)));
        if (!watches.isEmpty()) {
            captureWatch(top, location, ref, result);
        }
        if (profiling && enclosing != null) {
            // Executed inside another step's formula: leave a reference there, like the legacy nested leaf.
            recordChild(top, stepRef(enclosing), CallNode.referenceTo(StringPool.intern(top.getUri()),
                    StringPool.intern(ref), StringPool.intern(location.label())));
        }
        // This whole step — its own time plus any nested steps — is nested time for the step it ran inside.
        nestedStepNanos = nestedAtStepEnter + wall;
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
            recordChild(top, stepRef(location), CallNode.referenceTo(StringPool.intern(top.getUri()),
                    StringPool.intern(original.ref()), StringPool.intern(original.label())));
        }
        return true;
    }

    private static String stepRef(CurrentLocation location) {
        if (location.ref() != null) {
            return location.ref();
        }
        return location.label() != null ? location.label() : location.kind().getCode();
    }

    /**
     * Attach a leaf reference node to its caller frame while the tree is under the node cap.
     *
     * <p>Returned call frames reserve their slot on entry and attach themselves; this handles the reference
     * leaves a step leaves behind. Once the cap is reached the reference is dropped and the tree is flagged
     * truncated, so the profile can report that it is incomplete instead of exhausting memory.
     */
    private void recordChild(DebugFrame frame, @Nullable String callerRef, CallNode node) {
        // A reference leaf (a re-read of an already-executed step) is not a sub-call, so a dropped one only
        // flags the tree truncated (in admitNode) — it must not inflate the frame's not-retained sub-call count.
        if (admitNode()) {
            frame.recordExecutedChild(callerRef, node);
        }
    }

    /**
     * Reserve one node in the bounded tree. A node is admitted only while the global cap and the subtree budget
     * of every branch it hangs under still have room, so a huge run degrades to a truncated tree — fairly across
     * branches — instead of exhausting memory or letting one branch starve the rest. Flags the tree truncated
     * whenever a node is dropped.
     */
    private boolean admitNode() {
        if (recordedNodes >= maxTreeNodes) {
            treeTruncated = true;
            return false;
        }
        for (DebugFrame ancestor : stack) {
            if (ancestor.getBudget() <= 0) {
                treeTruncated = true;
                return false;
            }
        }
        recordedNodes++;
        for (DebugFrame ancestor : stack) {
            ancestor.decrementBudget();
        }
        return true;
    }

    /** Lower the retained-tree node cap. Test seam only: exercise truncation without a huge profiled run. */
    void setMaxTreeNodes(int maxTreeNodes) {
        this.maxTreeNodes = maxTreeNodes;
    }

    boolean isTreeTruncated() {
        return treeTruncated;
    }

    /** Record a watched cell's value if this step is watched (by its name or ref), unless the cap is hit. */
    private void captureWatch(DebugFrame frame, CurrentLocation location, String ref, @Nullable Object value) {
        String label = location.label();
        if (!watches.contains(ref) && (label == null || !watches.contains(label))) {
            return;
        }
        if (captures.size() >= MAX_WATCH_CAPTURES) {
            watchTruncated = true;
            return;
        }
        String name = label != null ? label : ref;
        // Keep the live value; it is deep-cloned and serialized to the rich parameter view on read, so it
        // renders like any other traced value (dates, arrays, spreadsheet results) instead of a raw toString.
        captures.add(new WatchCapture(name, frame.getName(), frame.getUri(), frame.getInvocationIndex(),
                framePath(), frame.getUri() + "#" + ref, value));
    }

    /** The call path from the root frame to the current frame, as display names, for a capture. */
    private List<String> framePath() {
        List<String> path = new ArrayList<>(stack.size());
        Iterator<DebugFrame> it = stack.descendingIterator();
        while (it.hasNext()) {
            path.add(it.next().getName());
        }
        return path;
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
        // Number each execution of the table so a watched cell's captures form a per-instance series. Counted
        // always (not only while watching), so instances stay correct even for a watch added mid-run.
        frame.setInvocationIndex(invocationCounts.merge(descriptor.uri(), 1, Integer::sum) - 1);
        if (pendingDispatch != null) {
            // This frame is the version the pending dispatcher chose; badge it and consume the dispatch so only
            // the immediate child carries it.
            frame.setDispatch(DispatchInfoFactory.of(pendingDispatch, pendingChosen));
            pendingDispatch = null;
            pendingChosen = null;
        }
        // Reserve this frame's slot in the bounded tree on ENTRY (top-down). A frame admitted here can always be
        // attached on exit — even after its own descendants have filled the cap. Reserving on attach instead
        // (bottom-up) drops a node whose subtree reached the cap, orphaning (losing) that whole recorded subtree.
        boolean recorded = profiling && admitNode();
        // Bound this frame's subtree by its parent's remaining budget and the per-branch cap, so a deep branch
        // entered early cannot use up the whole tree before its siblings run. A frame that was not admitted gets
        // no budget, so nothing is retained under it either.
        frame.setBudget(recorded ? (parent == null ? maxTreeNodes : Math.min(parent.getBudget(), MAX_BRANCH_NODES)) : 0);
        // A profiled sub-call the tree could not admit is one the caller made but the tree no longer keeps; count
        // it on the caller so its node can honestly report how many of its sub-calls were dropped.
        if (profiling && !recorded && parent != null) {
            parent.incrementNotRetained();
        }
        stack.push(frame);
        try {
            handleEvent(DebugEvent.ENTER, depth, descriptor.uri(), null, descriptor.name(),
                    frame.getInvocationIndex());
            R result = executor.invoke(target, params, env);
            frame.completeWith(result);
            // Time the frame the moment it finishes, before Step Out can suspend at its exit, so a completed
            // frame already on the stack carries its timing.
            frame.setDurationNanos(elapsed(enterNanos, parkedAtEnter));
            handleEvent(DebugEvent.EXIT, depth, descriptor.uri(), null, descriptor.name(),
                    frame.getInvocationIndex());
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
            // A frame unwound by a terminate neither completed nor failed, so it is skipped entirely — no
            // misleading zero-time entry in either the hotspots or the tree.
            if (profiling && (frame.isCompleted() || frame.getError() != null)) {
                // Aggregate this table's time on the fly for EVERY execution — independent of whether the node
                // is kept in the (capped) tree — so the hotspots overview stays accurate on a truncated run.
                long selfNanos = Math.max(0, frame.getDurationNanos() - frame.getChildNanos());
                tableStats.computeIfAbsent(frame.getUri(),
                                uri -> new TableAccumulator(uri, frame.getName(), frame.getKind()))
                        .add(frame.getDurationNanos(), selfNanos);
                if (parent != null) {
                    parent.addChildNanos(frame.getDurationNanos());
                }
                // Profiling also keeps the returned frame's structure (no values) so the executed tree survives
                // the pop. A returned root frame has no parent to hold it, so it becomes the completed tree. The
                // slot was reserved on entry (recorded), so a frame whose subtree filled the cap still attaches.
                if (recorded) {
                    CallNode node = frame.toCallNode(StringPool::intern);
                    if (parent != null) {
                        parent.recordExecutedChild(callerRef, node);
                    } else {
                        completedTree.set(node);
                    }
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
                             @Nullable String name, int instance) {
        if (channel.isTerminateRequested()) {
            throw new DebugTerminationError();
        }
        if (stepController.shouldSuspend(event, depth, uri, location, name, instance)) {
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
        return completedTree.get();
    }

    /** Per-table profiling stats accumulated over the run, for the hotspots overview; complete even when truncated. */
    List<TableProfile> profileStats() {
        return tableStats.values().stream().map(TableAccumulator::toProfile).toList();
    }

    /** The frame at the given stack index in the published snapshot, or {@code null} if out of range. */
    @Nullable
    DebugFrame frameAt(int index) {
        List<DebugFrame> current = published.get();
        return index >= 0 && index < current.size() ? current.get(index) : null;
    }

    /** Mutable accumulator for one table's aggregated time across every invocation in the run. */
    private static final class TableAccumulator {
        private final String uri;
        private final String name;
        private final FrameKind kind;
        private int count;
        private long totalNanos;
        private long selfNanos;

        private TableAccumulator(String uri, String name, FrameKind kind) {
            this.uri = uri;
            this.name = name;
            this.kind = kind;
        }

        private void add(long totalNanos, long selfNanos) {
            count++;
            this.totalNanos += totalNanos;
            this.selfNanos += selfNanos;
        }

        private TableProfile toProfile() {
            return new TableProfile(uri, name, kind, count, selfNanos, totalNanos);
        }
    }
}
