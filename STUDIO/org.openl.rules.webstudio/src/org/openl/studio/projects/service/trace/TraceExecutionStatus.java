package org.openl.studio.projects.service.trace;

/**
 * Status of trace execution.
 * <p>
 * These statuses are sent to the UI via WebSocket to indicate the
 * current state of an asynchronous trace execution task.
 * </p>
 *
 */
public enum TraceExecutionStatus {
    /** Trace execution request received, waiting to start */
    PENDING,
    /** Trace execution is in progress */
    STARTED,
    /** Trace execution completed successfully */
    COMPLETED,
    /** Trace execution failed with an error */
    ERROR
}
