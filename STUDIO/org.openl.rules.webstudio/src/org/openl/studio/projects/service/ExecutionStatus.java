package org.openl.studio.projects.service;

/**
 * Status of asynchronous execution (run, trace, etc.).
 * <p>
 * These statuses are sent to the UI via WebSocket to indicate the
 * current state of an asynchronous execution task.
 * </p>
 */
public enum ExecutionStatus {
    /** Execution request received, waiting to start */
    PENDING,
    /** Execution is in progress */
    STARTED,
    /** Execution completed successfully */
    COMPLETED,
    /** Execution was interrupted/cancelled by the user */
    INTERRUPTED,
    /** Execution failed with an error */
    ERROR
}
