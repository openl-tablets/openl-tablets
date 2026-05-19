package org.openl.studio.projects.service.project.compile;

/**
 * Lifecycle state of an asynchronous project compilation job.
 *
 * @author Vladyslav Pikus
 */
public enum CompilationStatus {

    /**
     * Job is created but has not started running yet.
     */
    PENDING,

    /**
     * Compilation is in progress.
     */
    RUNNING,

    /**
     * Compilation finished successfully.
     */
    SUCCEEDED,

    /**
     * Compilation finished with an unrecoverable error.
     */
    FAILED
}
