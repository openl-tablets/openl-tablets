package org.openl.rules.webstudio.projects.tests.executor;

import org.openl.rules.testmethod.TestUnitsResults;

public interface ProjectTestsExecutionProgressListener {

    /**
     * Called when the test execution status changes.
     *
     * @param status the new test execution status
     */
    void onStatusChanged(TestExecutionStatus status);

    /**
     * Called when a test unit has been executed.
     *
     * @param testUnitsResults the results of the executed test unit
     */
    void onTestUnitExecuted(TestUnitsResults testUnitsResults);

}
