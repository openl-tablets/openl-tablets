package org.openl.studio.projects.service.trace;

/**
 * Listener for trace execution progress updates.
 * <p>
 * Implementations receive notifications when trace execution status changes,
 * typically to relay progress to the UI via WebSocket.
 * </p>
 *
 */
public interface TraceExecutionProgressListener {

    /**
     * Called when the trace execution status changes.
     *
     * @param status the new status
     */
    void onStatusChanged(TraceExecutionStatus status);

    /**
     * Called when an error occurs during trace execution.
     *
     * @param message error message
     * @param cause   the cause of the error
     */
    default void onError(String message, Throwable cause) {
        onStatusChanged(TraceExecutionStatus.ERROR);
    }
}
