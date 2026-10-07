package org.openl.studio.projects.service;

/**
 * Listener for asynchronous execution progress updates.
 * <p>
 * Implementations receive notifications when execution status changes,
 * typically to relay progress to the UI via WebSocket.
 * </p>
 */
public interface ExecutionProgressListener {

    /**
     * Called when the execution status changes.
     *
     * @param status the new status
     */
    void onStatusChanged(ExecutionStatus status);

    /**
     * Called when an error occurs during execution.
     *
     * @param message error message
     * @param cause   the cause of the error
     */
    default void onError(String message, Throwable cause) {
        onStatusChanged(ExecutionStatus.ERROR);
    }
}
