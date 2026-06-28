package org.openl.rules.webstudio.web.trace.debug;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.trace.TreeBuildTracer;

/**
 * Drives one debug session: runs the rule on a dedicated virtual thread and exposes debugger controls.
 *
 * <p>The worker thread parks at breakpoints and step points while its JVM stack holds the live
 * computation. Control methods ({@link #command}, {@link #pause}, {@link #terminate}) are short and
 * return promptly; they never block on rule execution beyond a bounded timeout.
 */
@Slf4j
public final class TraceDebugger {

    private final DebugChannel channel = new DebugChannel();
    private final StepController stepController = new StepController();
    private final DebugHookImpl hook;
    private final DebugListener listener;

    private volatile @Nullable Thread worker;
    private volatile @Nullable Throwable error;
    private long startHaltCount;

    public TraceDebugger(DebugListener listener) {
        this(new DefaultSourceClassifier(), listener);
    }

    TraceDebugger(SourceClassifier classifier) {
        this(classifier, DebugListener.NOOP);
    }

    TraceDebugger(SourceClassifier classifier, DebugListener listener) {
        this.listener = listener;
        this.hook = new DebugHookImpl(classifier, stepController, channel, listener);
    }

    public void setBreakpoints(Set<String> uris) {
        stepController.setBreakpoints(uris);
    }

    public Set<String> getBreakpoints() {
        return stepController.getBreakpoints();
    }

    /**
     * Start execution on a fresh virtual thread.
     *
     * @param threadName  worker thread name
     * @param classLoader context classloader for the worker, or {@code null} to keep the current one
     * @param stopAtEntry suspend at the first frame instead of running to the first breakpoint
     * @param body        the rule execution to run
     */
    public void start(String threadName, @Nullable ClassLoader classLoader, boolean stopAtEntry, DebugBody body) {
        stepController.armInitial(stopAtEntry);
        startHaltCount = channel.haltCount();
        channel.markRunning();
        Thread thread = Thread.ofVirtual().name(threadName).unstarted(() -> run(classLoader, body));
        this.worker = thread;
        thread.start();
    }

    private void run(@Nullable ClassLoader classLoader, DebugBody body) {
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        if (classLoader != null) {
            current.setContextClassLoader(classLoader);
        }
        TreeBuildTracer.enableDebug(hook);
        DebugStatus terminal;
        try {
            body.execute();
            channel.markCompleted();
            terminal = DebugStatus.COMPLETED;
        } catch (DebugTerminationError e) {
            channel.markTerminated();
            terminal = DebugStatus.TERMINATED;
        } catch (Throwable t) {
            error = t;
            log.debug("Debug session failed", t);
            channel.markError();
            terminal = DebugStatus.ERROR;
        } finally {
            TreeBuildTracer.disableDebug();
            current.setContextClassLoader(previous);
        }
        listener.onStatusChanged(terminal);
    }

    public DebugStatus status() {
        return channel.status();
    }

    public @Nullable Throwable error() {
        return error;
    }

    /** Live execution stack from the most recent suspension, ordered root to current frame. */
    public List<DebugFrame> stack() {
        return hook.snapshot();
    }

    public @Nullable DebugFrame frameAt(int index) {
        return hook.frameAt(index);
    }

    /** Wait for the worker to reach its first suspend or a terminal state. */
    public DebugStatus awaitInitialHalt(long timeoutMillis) {
        return channel.awaitHalt(startHaltCount, timeoutMillis);
    }

    /** Resume with a command and wait, bounded by the timeout, for the next suspend or terminal state. */
    public DebugStatus command(DebugCommand command, long timeoutMillis) {
        long before = channel.haltCount();
        channel.postCommand(command);
        return channel.awaitHalt(before, timeoutMillis);
    }

    /** Resume to the next breakpoint without waiting. */
    public void resume() {
        channel.postCommand(DebugCommand.RESUME);
    }

    /** Request an asynchronous suspend at the next safepoint. */
    public void pause() {
        stepController.requestPause();
    }

    /** Cancel the session, interrupting and briefly joining the worker. */
    public void terminate(long joinMillis) {
        channel.requestTerminate();
        Thread thread = worker;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(joinMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
