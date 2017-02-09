package org.openl.rules.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.openl.CompiledOpenClass;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;

/**
 * Run OpenL tests
 * 
 * @author Yury Molchan
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class TestMojo extends BaseOpenLMojo {
    private static final String SEPARATOR = "-------------------------------------------------------------";

    /**
     * Set this to 'true' to skip running OpenL tests.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipTests) {
            return;
        }
        CompiledOpenClass openLRules;
        try {
            openLRules = compileOpenLRules();
        } catch (Exception e) {
            throw new MojoFailureException("Failed to compile OpenL project", e);
        }

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;
        logHeader();

        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        for (TestSuiteMethod test : tests) {
            try {
                TestUnitsResults result = new TestSuite(test).invokeSequentially(openClass, 1L);

                runTests += result.getNumberOfTestUnits();
                failedTests += result.getNumberOfFailures();
                getLog().info(result.toString());
            } catch (Exception e) {
                getLog().error(e);
                errors++;
            }
        }

        logFooter(runTests, failedTests, errors);
        if (failedTests > 0 || errors > 0) {
            throw new MojoFailureException("There are OpenL errors");
        }
    }

    private void logHeader() {
        if (getLog().isInfoEnabled()) {
            getLog().info(SEPARATOR);
            getLog().info("OPENL TESTS");
            getLog().info(SEPARATOR);
        }
    }

    private void logFooter(int run, int failures, int errors) {
        if (getLog().isInfoEnabled()) {
            getLog().info("Results:");
            getLog().info(String.format("Tests run: %d, Failures: %d, Errors: %d", run, failures, errors));
            getLog().info(SEPARATOR);
        }
    }
}
