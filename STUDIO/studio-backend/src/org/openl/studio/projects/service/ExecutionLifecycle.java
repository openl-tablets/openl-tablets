package org.openl.studio.projects.service;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.openl.util.StringUtils;

/**
 * Runs a task of an asynchronous execution and tells a listener how it went.
 *
 * <p>The listener hears {@link ExecutionStatus#STARTED} before the task runs, and one of
 * {@link ExecutionStatus#COMPLETED}, {@link ExecutionStatus#INTERRUPTED} or an error afterwards.
 *
 * <p>A task is interrupted in either of two ways, and they end differently. One that gave up on the
 * interruption has nothing to give, so its future completes with no result. One that reached its end
 * while it was being asked to stop has a whole result, and its future completes with it - the listener
 * is told of the interruption all the same, because the work was asked to stop.
 */
public final class ExecutionLifecycle {

    private ExecutionLifecycle() {
    }

    /**
     * Executes a task and reports its outcome to the listener.
     *
     * @param listener progress listener
     * @param task     the task to execute
     * @param <T>      result type
     * @return a future that completes with the task result, or fails with what the task threw
     */
    public static <T> CompletableFuture<T> execute(ExecutionProgressListener listener, Callable<T> task) {
        listener.onStatusChanged(ExecutionStatus.STARTED);
        try {
            var result = task.call();

            if (Thread.currentThread().isInterrupted()) {
                // The task ran to its end while it was being asked to stop, so what it found is whole
                // and is handed over; the listener hears that the stop came too late to take effect.
                listener.onStatusChanged(ExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(result);
            }

            listener.onStatusChanged(ExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            if (isInterrupted(e)) {
                listener.onStatusChanged(ExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(null);
            }
            // A listener is told why it failed; an exception that says nothing is named by its own type,
            // so that the reason is never missing.
            var reason = StringUtils.isBlank(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
            listener.onError(reason, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Tells whether a failure was caused by the thread being interrupted.
     *
     * @param e the failure to inspect
     * @return true when interruption is the cause anywhere in the chain
     */
    public static boolean isInterrupted(Throwable e) {
        var cause = e;
        while (cause != null) {
            if (cause instanceof InterruptedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
