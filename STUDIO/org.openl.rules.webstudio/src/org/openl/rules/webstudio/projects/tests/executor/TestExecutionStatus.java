package org.openl.rules.webstudio.projects.tests.executor;

public enum TestExecutionStatus {
    /**
     * The test execution is pending and has not started yet.
     */
    PENDING,

    /**
     * The test execution has started.
     */
    STARTED,

    /**
     * The test execution has been completed successfully.
     */
    COMPLETED,

    /**
     * The test execution has been interrupted.
     */
    INTERRUPTED
}
