package org.openl.rules.maven;

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
    /**
     * Set this to 'true' to skip running OpenL tests.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    @Override
    public String execute(CompiledOpenClass openLRules) {
        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        for (TestSuiteMethod test : tests) {
            try {
                TestUnitsResults result = new TestSuite(test).invokeSequentially(openClass, 1L);

                runTests += result.getNumberOfTestUnits();
                failedTests += result.getNumberOfFailures();
                info(result.toString());
            } catch (Exception e) {
                error(e);
                errors++;
            }
        }

        info("Results:");
        info(String.format("Tests run: %d, Failures: %d, Errors: %d", runTests, failedTests, errors));
        if (failedTests > 0 || errors > 0) {
            return "There are errors in the OpenL tests";
        }
        return null;
    }

    @Override
    String getHeader() {
        return "OPENL TESTS";
    }

    @Override
    boolean isDisabled() {
        return skipTests;
    }
}
