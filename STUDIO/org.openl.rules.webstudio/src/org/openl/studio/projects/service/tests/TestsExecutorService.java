package org.openl.studio.projects.service.tests;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;

/**
 * Runs the tests of a project in the background.
 *
 * <p>The results keep every case the way {@link RetainedTestUnit} does. What a case reports stays, and a value with
 * inner structure that it returned or compared is given back when the application runs short of memory. The case
 * runs again for such a value.
 */
public interface TestsExecutorService {

    /**
     * Run all tests in the project.
     *
     * @param listener execution progress listener
     * @param projectModel project model
     * @param currentOpenedModule if true, run tests only in the currently opened module; otherwise, run tests in all modules
     * @return a future that will be completed with the list of test units results
     */
    CompletableFuture<List<TestUnitsResults>> runAll(ProjectTestsExecutionProgressListener listener,
                                                     ProjectModel projectModel,
                                                     boolean currentOpenedModule);

    /**
     * Run specified test table and test ranges.
     *
     * @param listener execution progress listener
     * @param projectModel project model
     * @param table the test table to run
     * @param testRanges the test ranges to run
     * @param currentOpenedModule if true, run tests only in the currently opened module; otherwise, run tests in all modules
     * @return a future that will be completed with the list of test units results
     */
    CompletableFuture<List<TestUnitsResults>> runSingle(ProjectTestsExecutionProgressListener listener,
                                                      ProjectModel projectModel,
                                                      IOpenLTable table,
                                                      String testRanges,
                                                      boolean currentOpenedModule);

    /**
     * Run all tests for the specified table.
     *
     * @param listener execution progress listener
     * @param projectModel project model
     * @param table the table to run tests for
     * @param currentOpenedModule if true, run tests only in the currently opened module; otherwise, run tests in all modules
     * @return a future that will be completed with the list of test units results
     */
    CompletableFuture<List<TestUnitsResults>> runAllForTable(ProjectTestsExecutionProgressListener listener,
                                                           ProjectModel projectModel,
                                                           IOpenLTable table,
                                                           boolean currentOpenedModule);

}
