package org.openl.studio.projects.service.run;

/**
 * Status of run execution.
 * <p>
 * These statuses are sent to the UI via WebSocket to indicate the
 * current state of an asynchronous run execution task.
 * </p>
 */
public enum RunExecutionStatus {
    /** Run execution request received, waiting to start */
    PENDING,
    /** Run execution is in progress */
    STARTED,
    /** Run execution completed successfully */
    COMPLETED,
    /** Run execution was interrupted/cancelled by the user */
    INTERRUPTED,
    /** Run execution failed with an error */
    ERROR
}
