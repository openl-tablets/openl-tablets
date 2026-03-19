package org.openl.studio.projects.service.run;

/**
 * Listener for run execution progress updates.
 * <p>
 * Implementations receive notifications when run execution status changes,
 * typically to relay progress to the UI via WebSocket.
 * </p>
 */
public interface RunExecutionProgressListener {

    /**
     * Called when the run execution status changes.
     *
     * @param status the new status
     */
    void onStatusChanged(RunExecutionStatus status);

    /**
     * Called when an error occurs during run execution.
     *
     * @param message error message
     * @param cause   the cause of the error
     */
    default void onError(String message, Throwable cause) {
        onStatusChanged(RunExecutionStatus.ERROR);
    }
}
