package org.openl.studio.projects.service.tests;

import org.jspecify.annotations.Nullable;

import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;

/**
 * Runs the cases of a test run again, for the values the run no longer keeps.
 *
 * <p>A run gives back the values with inner structure when the application runs short of memory. A case is then run
 * again to have them: against the project as it was compiled for the run, and in the same scope - the whole
 * project, or the module that was open. What it returns goes to whoever asked and is not kept. The run the session
 * keeps stays as it is, and nothing is announced.
 *
 * <p>Once the project is compiled again, the cases of the run no longer answer for it, and nothing is run. Editing
 * the project and opening another module both compile it again.
 */
@FunctionalInterface
public interface TestsRerun {

    /**
     * Runs one case again, the case alone.
     *
     * @param method the test table the case belongs to
     * @param test   the case
     * @return the case as it ran, or {@code null} when the project was compiled again since the run
     */
    @Nullable ITestUnit runCase(TestSuiteMethod method, TestDescription test);

    /**
     * Runs cases again the way the run that starts now runs them.
     *
     * @param model               the project the run runs in
     * @param currentOpenedModule whether the run is of the module that is open rather than of the whole project
     */
    static TestsRerun of(ProjectModel model, boolean currentOpenedModule) {
        var compiled = currentOpenedModule ? model.getOpenedModuleCompiledOpenClass() : model.getCompiledOpenClass();
        return (method, test) -> {
            var results = model.runTest(new TestSuite(method, test.getIndex()), currentOpenedModule, compiled);
            return results == null || results.getTestUnits().isEmpty() ? null : results.getTestUnits().getFirst();
        };
    }
}
