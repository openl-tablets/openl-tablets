package org.openl.rules.maven;

import static org.openl.rules.testmethod.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestStatus.TR_OK;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.testmethod.*;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.types.IOpenClass;
import org.openl.types.impl.ThisField;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;

/**
 * Run OpenL tests
 *
 * @author Yury Molchan
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public final class TestMojo extends BaseOpenLMojo {
    private static final String FAILURE = "<<< FAILURE!";
    private static final String ERROR = "<<< ERROR!";
    /**
     * Set this to 'true' to skip running OpenL tests.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    @Parameter(defaultValue = "${project.build.testSourceDirectory}/../openl")
    private File testSourceDirectory;

    /**
     * Base directory where all reports are written to. Reports are surefire format compatible.
     */
    @Parameter(defaultValue = "${project.build.directory}/openl-test-reports")
    private File reportsDirectory;

    /**
     * Thread count to run test cases. The parameter is as follows:
     * <ul>
     * <li>4 - Runs tests with 4 threads</li>
     * <li>1.5C - 1.5 thread per cpu core</li>
     * <li>none - Run tests sequentially (don't create threads to run tests)</li>
     * <li>auto - Threads count will be configured automatically</li>
     * </ul>
     * Default value is "auto".
     */
    @Parameter(defaultValue = "auto")
    private String threadCount;

    /**
     * Compile the project in Single module mode. If true each module will be compiled in sequence and tests from that
     * module will be run. Needed for big projects. If false all modules will be compiled at once and all tests from all
     * modules will be run. By default false.
     */
    @Parameter(defaultValue = "false")
    private boolean singleModuleMode;

    /**
     * If you want to override some parameters, define them here.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    private final TestRunner testRunner = new TestRunner(BaseTestUnit.Builder.getInstance());

    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        Summary summary = runAllTests(sourcePath, hasDependencies);

        info("");
        info("Results:");
        if (summary.getFailedTests() > 0) {
            info("");
            info("Failed Tests:");
            for (String failure : summary.getSummaryFailures()) {
                info("  ", failure);
            }
        }
        if (summary.getErrors() > 0) {
            info("");
            info("Tests in error:");
            for (String error : summary.getSummaryErrors()) {
                info("  ", error);
            }
        }

        info("");
        info("Total tests run: ",
            summary.getRunTests(),
            ", Failures: ",
            summary.getFailedTests(),
            ", Errors: ",
            summary.getErrors());
        info("");
        if (summary.getFailedTests() > 0 || summary.getErrors() > 0) {
            throw new MojoFailureException("There are errors in the OpenL tests");
        } else if (summary.isHasCompilationErrors()) {
            throw new MojoFailureException("There are compilation errors in the OpenL tests ");
        }
    }

    private Summary runAllTests(String sourcePath, boolean hasDependencies) throws IOException,
                                                                            RulesInstantiationException,
                                                                            ProjectResolvingException,
                                                                            ClassNotFoundException {

        String path = sourcePath;

        if (testSourceDirectory.isDirectory() && !CollectionUtils.isEmpty(testSourceDirectory.list())) {
            File destination = new File(workspaceFolder, project.getArtifactId());
            debug("Destination path: ", destination.getPath());

            info("Copying main OpenL sources to workspace...");
            FileUtils.copy(new File(sourcePath), destination);
            info("Copying test OpenL sources to workspace...");
            FileUtils.copy(testSourceDirectory, destination);

            try {
                path = destination.getCanonicalPath();
            } catch (Exception e) {
                warn("The path to OpenL directory in workspace cannot be converted to canonical form.");
                path = destination.getPath();
            }
        }

        return singleModuleMode ? executeModuleByModule(path, hasDependencies)
                                : executeAllAtOnce(path, hasDependencies);
    }

    private Summary executeAllAtOnce(String sourcePath, boolean hasDependencies) throws MalformedURLException,
                                                                                 RulesInstantiationException,
                                                                                 ProjectResolvingException,
                                                                                 ClassNotFoundException {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(false)
                .setExternalParameters(externalParameters)
                .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            return executeTests(openLRules);
        } finally {
            releaseResources(classLoader);
        }
    }

    private Summary executeModuleByModule(String sourcePath, boolean hasDependencies) throws MalformedURLException,
                                                                                      RulesInstantiationException,
                                                                                      ProjectResolvingException,
                                                                                      ClassNotFoundException {
        ProjectDescriptor pd = ProjectResolver.instance().resolve(new File(sourcePath));
        if (pd == null) {
            throw new ProjectResolvingException("Failed to resolve project. Defined location is not an OpenL project.");
        }

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        List<String> summaryFailures = new ArrayList<>();
        List<String> summaryErrors = new ArrayList<>();
        boolean hasCompilationErrors = false;

        List<Module> modules = new ArrayList<>(pd.getModules());
        // Set higher priority to modules containing "test" keyword.
        Collections.sort(modules, new Comparator<Module>() {
            @Override
            public int compare(Module o1, Module o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                boolean isTest1 = name1.toLowerCase().contains("test");
                boolean isTest2 = name2.toLowerCase().contains("test");
                if (isTest1 == isTest2) {
                    return name1.compareTo(name2);
                }

                return isTest1 ? -1 : 1;
            }
        });

        for (Module module : modules) {
            URL[] urls = toURLs(classpath);
            ClassLoader classLoader = null;
            try {
                classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

                SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
                if (hasDependencies) {
                    builder.setWorkspace(workspaceFolder.getPath());
                }
                SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(false)
                    .setModule(module.getName())
                    .setExternalParameters(externalParameters)
                    .build();

                info("Searching tests in the module '", module.getName(), "'...");
                CompiledOpenClass openLRules = factory.getCompiledOpenClass();
                Summary summary = executeTests(openLRules);

                runTests += summary.getRunTests();
                failedTests += summary.getFailedTests();
                errors += summary.getErrors();
                summaryFailures.addAll(summary.getSummaryFailures());
                summaryErrors.addAll(summary.getSummaryErrors());
                hasCompilationErrors |= summary.isHasCompilationErrors();
            } finally {
                releaseResources(classLoader);
            }
        }

        return new Summary(runTests, failedTests, errors, summaryFailures, summaryErrors, hasCompilationErrors);
    }

    private Summary executeTests(CompiledOpenClass openLRules) {
        IOpenClass openClass = openLRules.getOpenClassWithErrors();

        if (openLRules.hasErrors()) {
            error("");
            error("There are compilation errors. It can affect test execution.");
            Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(openLRules.getMessages(), Severity.ERROR);
            int i = 0;
            for (OpenLMessage message : errorMessages) {
                String location = message.getSourceLocation() == null ? "" : " at " + message.getSourceLocation();
                error((i + 1) + ". '", message.getSummary(), "'", location);
                i++;
            }
            error("");
        }

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        List<String> summaryFailures = new ArrayList<>();
        List<String> summaryErrors = new ArrayList<>();

        TestSuiteExecutor testSuiteExecutor = createTestSuiteExecutor();

        try {
            TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
            for (TestSuiteMethod test : tests) {
                String moduleName = test.getModuleName();
                try {
                    info("");
                    String moduleInfo = moduleName == null ? "" : " from the module " + moduleName;
                    info("Running ", test.getName(), moduleInfo);
                    TestUnitsResults result;
                    if (testSuiteExecutor == null) {
                        result = new TestSuite(test, testRunner).invokeSequentially(openClass, 1L);
                    } else {
                        result = new TestSuite(test, testRunner).invokeParallel(testSuiteExecutor, openClass, 1L);
                    }
                    new JUnitReportWriter(reportsDirectory).write(result);

                    int suitTests = result.getNumberOfTestUnits();
                    int suitFailures = result.getNumberOfAssertionFailures();
                    int suitErrors = result.getNumberOfErrors();

                    info("Tests run: ",
                        suitTests,
                        ", Failures: ",
                        suitFailures,
                        ", Errors: ",
                        suitErrors,
                        ". Time elapsed: ",
                        formatTime(result.getExecutionTime()),
                        " sec.",
                        result.getNumberOfFailures() > 0 ? " " + FAILURE : "");

                    if (result.getNumberOfFailures() > 0) {
                        showFailures(test, result, summaryFailures, summaryErrors);
                    }

                    runTests += suitTests;
                    failedTests += suitFailures;
                    errors += suitErrors;
                } catch (Exception e) {
                    error(e);
                    errors++;
                    String modulePrefix = moduleName == null ? "" : moduleName + ".";
                    Throwable cause = ExceptionUtils.getRootCause(e);
                    if (cause == null) {
                        cause = e;
                    }
                    summaryErrors.add(modulePrefix + test.getName() + " " + cause.getClass().getName());
                }
            }

            return new Summary(runTests, failedTests, errors, summaryFailures, summaryErrors, openLRules.hasErrors());
        } finally {
            if (testSuiteExecutor != null) {
                testSuiteExecutor.destroy();
            }
        }
    }

    private void showFailures(TestSuiteMethod test,
            TestUnitsResults result,
            List<String> summaryFailures,
            List<String> summaryErrors) {
        int num = 1;
        String moduleName = test.getModuleName();
        String modulePrefix = moduleName == null ? "" : moduleName + ".";

        for (ITestUnit testUnit : result.getTestUnits()) {
            TestStatus status = testUnit.getResultStatus();
            if (status != TR_OK) {
                String failureType = status == TR_NEQ ? FAILURE : ERROR;
                String description = testUnit.getDescription();

                info("  Test case: #", num,
                        ITestUnit.DEFAULT_DESCRIPTION.equals(description) ? "" : " (" + description + ")",
                    ". Time elapsed: ",
                    formatTime(testUnit.getExecutionTime()),
                    " sec. ",
                    failureType);

                if (status == TR_NEQ) {
                    StringBuilder summaryBuilder = new StringBuilder(modulePrefix + test.getName() + "#" + num);

                    List<ComparedResult> comparisonResults = testUnit.getComparisonResults();
                    int rowNum = 0;
                    for (ComparedResult comparisonResult : comparisonResults) {
                        if (comparisonResult.getStatus() != TR_OK) {
                            if (comparisonResult.getFieldName().equals(ThisField.THIS)) {
                                info("    Expected: <" + comparisonResult
                                    .getExpectedValue() + "> but was: <" + comparisonResult.getActualValue() + ">");
                                summaryBuilder.append(" expected: <")
                                    .append(comparisonResult.getExpectedValue())
                                    .append("> but was <")
                                    .append(comparisonResult.getActualValue())
                                    .append(">");
                            } else {
                                if (rowNum > 0) {
                                    summaryBuilder.append(",");
                                }
                                info("    Field " + comparisonResult.getFieldName() + " expected: <" + comparisonResult
                                    .getExpectedValue() + "> but was: <" + comparisonResult.getActualValue() + ">");

                                summaryBuilder.append(" field ")
                                    .append(comparisonResult.getFieldName())
                                    .append(" expected: <")
                                    .append(comparisonResult.getExpectedValue())
                                    .append("> but was <")
                                    .append(comparisonResult.getActualValue())
                                    .append(">");
                            }
                            rowNum++;
                        }
                    }
                    summaryFailures.add(summaryBuilder.toString());
                } else {
                    Throwable error = (Throwable) testUnit.getActualResult();
                    info("  Error: ", error, "\n", ExceptionUtils.getStackTrace(error));
                    Throwable cause = ExceptionUtils.getRootCause(error);
                    if (cause == null) {
                        cause = error;
                    }
                    summaryErrors.add(modulePrefix + test.getName() + "#" + num + " " + cause.getClass().getName());
                }
            }
            num++;
        }
    }

    @Override
    String getHeader() {
        return "OPENL TESTS";
    }

    @Override
    boolean isDisabled() {
        return skipTests;
    }

    private String formatTime(long nanoseconds) {
        double time = (double) nanoseconds / 1000_000_000;
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        df.applyPattern("#.###");
        return time < 0.001 ? "< 0.001" : df.format(time);
    }

    private TestSuiteExecutor createTestSuiteExecutor() {
        int threads;

        switch (threadCount) {
            case "none":
                return null;
            case "auto":
                // Can be changed in the future
                threads = Runtime.getRuntime().availableProcessors() + 2;
                break;
            default:
                if (threadCount.matches("\\d+")) {
                    threads = Integer.parseInt(threadCount);
                } else if (threadCount.matches("\\d[\\d.]*[cC]")) {
                    float multiplier = Float.parseFloat(threadCount.substring(0, threadCount.length() - 1));
                    threads = (int) (multiplier * Runtime.getRuntime().availableProcessors());
                } else {
                    throw new IllegalArgumentException("Incorrect thread count '" + threadCount + "'");
                }
                break;
        }
        info("Run tests using ", threads, " threads.");

        return new TestSuiteExecutor(threads);
    }

    private static class Summary {
        private final int runTests;
        private final int failedTests;
        private final int errors;
        private final List<String> summaryFailures;
        private final List<String> summaryErrors;
        private final boolean hasCompilationErrors;

        public Summary(int runTests,
                int failedTests,
                int errors,
                List<String> summaryFailures,
                List<String> summaryErrors,
                boolean hasCompilationErrors) {
            this.runTests = runTests;
            this.failedTests = failedTests;
            this.errors = errors;
            this.summaryFailures = Collections.unmodifiableList(summaryFailures);
            this.summaryErrors = Collections.unmodifiableList(summaryErrors);
            this.hasCompilationErrors = hasCompilationErrors;
        }

        public int getRunTests() {
            return runTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public int getErrors() {
            return errors;
        }

        public List<String> getSummaryFailures() {
            return summaryFailures;
        }

        public List<String> getSummaryErrors() {
            return summaryErrors;
        }

        public boolean isHasCompilationErrors() {
            return hasCompilationErrors;
        }
    }
}
