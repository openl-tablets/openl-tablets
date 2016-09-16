package org.openl.rules.maven;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.types.IOpenClass;

/**
 * Run OpenL tests
 * 
 * @author NSamatov
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
        if (getLog().isInfoEnabled()) {
            getLog().info(String.format("Testing the project in %s", openlOutputDirectory));
        }

        ProjectDescriptor projectDescriptor;
        try {
            projectDescriptor = ProjectHelpers.resolveProject(openlOutputDirectory);
            if (projectDescriptor == null) {
                throw new MojoFailureException(String.format("Cannot find OpenL project in directory %s",
                    openlOutputDirectory));
            }
        } catch (ProjectResolvingException e) {
            if (getLog().isErrorEnabled()) {
                getLog().error(e.getMessage(), e);
            }
            throw new MojoFailureException(String.format("Cannot resolve OpenL project in directory %s", openlOutputDirectory), e);
        }

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;
        logHeader();

        IDependencyManager dependencyManager = ProjectHelpers.getDependencyManager(projectDescriptor);

        StringBuilder stringBuilder = new StringBuilder();
        for (Module module : projectDescriptor.getModules()) {
            RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module, false,
                dependencyManager);
            try {
                CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();

                IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
                TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
                for (TestSuiteMethod test : tests) {
                    TestUnitsResults result = new TestSuite(test).invokeSequentially(openClass, 1L);
                    runTests += result.getNumberOfTestUnits();
                    if (result.getNumberOfFailures() > 0) {
                        failedTests += result.getNumberOfFailures();
                        result.printFailedUnits(stringBuilder);
                    }
                }
            } catch (RulesInstantiationException e) {
                errors++;
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                stringBuilder.append("Failed to compile module '")
                    .append(module.getName())
                    .append("'\n")
                    .append(sw.toString());
            }
        }

        if (failedTests > 0 || errors > 0) {
            if (getLog().isErrorEnabled()) {
                getLog().error(stringBuilder.toString());
            }
            logFooter(runTests, failedTests, errors);
            throw new MojoFailureException("There are OpenL errors");
        }

        logFooter(runTests, failedTests, errors);
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
