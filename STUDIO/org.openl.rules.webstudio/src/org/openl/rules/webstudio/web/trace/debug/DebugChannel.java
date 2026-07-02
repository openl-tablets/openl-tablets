package org.openl.rules.webstudio.web.trace.debug;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

/**
 * Rendezvous between the controller (HTTP) thread and the parked worker thread.
 *
 * <p>The worker halts by calling {@link #awaitCommand()}, which publishes the suspended state and
 * blocks until the controller posts a command or requests termination. The controller drives the
 * worker with {@link #postCommand} and waits for the next halt with {@link #awaitHalt}. A monotonic
 * halt counter lets the controller wait for the <em>next</em> halt rather than observing the halt it
 * resumed from.
 *
 * <p>While the worker is blocked here it runs no rule code, so its live object graph is stable and the
 * controller may read or freeze the stack safely.
 */
final class DebugChannel {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition commandPosted = lock.newCondition();
    private final Condition halted = lock.newCondition();

    private DebugStatus status = DebugStatus.PENDING;
    private @Nullable DebugCommand pending;
    private boolean terminateRequested;
    private long haltCount;

    void markRunning() {
        lock.lock();
        try {
            status = DebugStatus.RUNNING;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Halt the worker, publish the suspended state, and block until resumed.
     *
     * @return the command the controller posted to continue
     * @throws DebugTerminationError if termination was requested while halted
     */
    DebugCommand awaitCommand() {
        lock.lock();
        try {
            if (terminateRequested) {
                throw new DebugTerminationError();
            }
            status = DebugStatus.SUSPENDED;
            haltCount++;
            halted.signalAll();
            while (pending == null && !terminateRequested) {
                commandPosted.awaitUninterruptibly();
            }
            if (terminateRequested) {
                throw new DebugTerminationError();
            }
            DebugCommand command = pending;
            pending = null;
            status = DebugStatus.RUNNING;
            return command;
        } finally {
            lock.unlock();
        }
    }

    boolean isTerminateRequested() {
        lock.lock();
        try {
            return terminateRequested;
        } finally {
            lock.unlock();
        }
    }

    void markCompleted() {
        finish(DebugStatus.COMPLETED);
    }

    void markError() {
        finish(DebugStatus.ERROR);
    }

    void markTerminated() {
        finish(DebugStatus.TERMINATED);
    }

    private void finish(DebugStatus terminal) {
        lock.lock();
        try {
            status = terminal;
            haltCount++;
            halted.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Post a resume command to a suspended worker. Ignored if not suspended. */
    void postCommand(DebugCommand command) {
        lock.lock();
        try {
            if (status != DebugStatus.SUSPENDED) {
                return;
            }
            pending = command;
            status = DebugStatus.RUNNING;
            commandPosted.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void requestTerminate() {
        lock.lock();
        try {
            terminateRequested = true;
            commandPosted.signalAll();
        } finally {
            lock.unlock();
        }
    }

    DebugStatus status() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait until the worker reaches a halt newer than {@code afterHalt} or a terminal state, bounded by
     * the timeout.
     *
     * @return the status observed when waiting ended
     */
    DebugStatus awaitHalt(long afterHalt, long timeoutMillis) {
        lock.lock();
        try {
            long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while (haltCount <= afterHalt && !status.isTerminal() && remaining > 0) {
                remaining = halted.awaitNanos(remaining);
            }
            return status;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return status;
        } finally {
            lock.unlock();
        }
    }

    long haltCount() {
        lock.lock();
        try {
            return haltCount;
        } finally {
            lock.unlock();
        }
    }
}
