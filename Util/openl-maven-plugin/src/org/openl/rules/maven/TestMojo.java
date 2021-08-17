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
import java.util.*;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
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
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ThisField;

/**
 * Runs OpenL Tablets tests.
 *
 * @author Yury Molchan
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public final class TestMojo extends BaseOpenLMojo {
    private static final String FAILURE = "<<< FAILURE";
    private static final String ERROR = "<<< ERROR";
    private static final int MAX_MODULES_IN_QUEUE = 10;

    /**
     * Parameter to skip running OpenL Tablets tests if it set to 'true'.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    /**
     * Directory containing OpenL Tablets sources to be used in testing OpenL Tablets rules.
     */
    @Parameter(defaultValue = "${project.build.testSourceDirectory}/../openl")
    private File testSourceDirectory;

    /**
     * Base directory where all reports are saved. Reports are surefire format compatible.
     */
    @Parameter(defaultValue = "${project.build.directory}/openl-test-reports")
    private File reportsDirectory;

    /**
     * File format of the test reports. Supported values: junit4 or xlsx.
     */
    @Parameter(defaultValue = "junit4")
    private ReportFormat[] reportsFormat;

    /**
     * Thread count to run test cases. The values are as follows:
     * <ul>
     * <li>4 - Runs tests with four threads.</li>
     * <li>1.5C - Runs tests with 1.5 thread per CPU core.</li>
     * <li>none - Runs tests sequentially. No extra threads for running tests are created.</li>
     * <li>auto - Automatically configures thread count.</li>
     * </ul>
     */
    @Parameter(defaultValue = "auto")
    private String threadCount;

    /**
     * Parameter for compiling the project in the single module mode, where each module is compiled in sequence and
     * tests from that module are run. This parameter is beneficial for big projects. If this parameter is set to false,
     * all modules are compiled at once and all tests from all modules are run.
     */
    @Parameter(defaultValue = "false")
    private boolean singleModuleMode;

    /**
     * Parameter for compiling the project in the smart mode to save the memory, where each module is compiled in
     * sequence and tests from that module are run. This parameter is beneficial for big projects.
     */
    @Parameter(defaultValue = "0")
    private int maxModulesInMemory;

    /**
     * Additional options for testing defined externally.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    private TestRunner testRunner;

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
            throw new MojoFailureException("There are errors in the OpenL tests.");
        } else if (summary.isHasCompilationErrors()) {
            throw new MojoFailureException("There are compilation errors in the OpenL tests.");
        }
    }

    private Summary runAllTests(String sourcePath,
            boolean hasDependencies) throws IOException, RulesInstantiationException, ProjectResolvingException {

        String testSourcePath = sourcePath;
        String mainSourcePath = null;

        File testDir = testSourceDirectory.getCanonicalFile();
        if (testDir.isDirectory() && ProjectResolver.getInstance().isRulesProject(testDir) != null) {
            mainSourcePath = sourcePath;

            try {
                testSourcePath = testDir.getCanonicalPath();
            } catch (Exception e) {
                warn("The path to OpenL test directory cannot be converted to canonical form.");
                testSourcePath = testDir.getPath();
            }
        }

        return singleModuleMode || maxModulesInMemory > 0 ? executeModuleByModule(testSourcePath,
            mainSourcePath,
            hasDependencies) : executeAllAtOnce(testSourcePath, mainSourcePath, hasDependencies);
    }

    private Summary executeAllAtOnce(String testSourcePath,
            String mainSourcePath,
            boolean hasDependencies) throws MalformedURLException,
                                     RulesInstantiationException,
                                     ProjectResolvingException {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            if (mainSourcePath != null) {
                builder.setProjectDependencies(mainSourcePath);
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(testSourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(false)
                .setExternalParameters(externalParameters)
                .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            return executeTests(openLRules);
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    private Summary executeModuleByModule(String testSourcePath,
            String mainSourcePath,
            boolean hasDependencies) throws MalformedURLException, ProjectResolvingException {
        ProjectDescriptor pd = ProjectResolver.getInstance().resolve(new File(testSourcePath));
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
        modules.sort(Comparator.comparing(Module::getName));

        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
        if (mainSourcePath != null) {
            builder.setProjectDependencies(mainSourcePath);
        }
        if (hasDependencies) {
            builder.setWorkspace(workspaceFolder.getPath());
        }
        SimpleProjectEngineFactory<?> factory = builder.setProject(testSourcePath)
            .setClassLoader(classLoader)
            .setExecutionMode(false)
            .setExternalParameters(externalParameters)
            .build();

        Deque<IDependency> queueToReset = new ArrayDeque<>();
        for (Module module : modules) {
            IDependency dependency = new Dependency(DependencyType.MODULE,
                new IdentifierNode(DependencyType.MODULE.name(), null, module.getName(), null));
            try {
                info("");
                info("Searching tests in module '", module.getName(), "'...");
                CompiledOpenClass compiledOpenClass;
                try {
                    CompiledDependency compiledDependency = factory.getDependencyManager().loadDependency(dependency);
                    compiledOpenClass = compiledDependency.getCompiledOpenClass();
                } catch (OpenLCompilationException e) {
                    Collection<OpenLMessage> messages = new LinkedHashSet<>();
                    for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(e)) {
                        String message = String.format("Failed to load module '%s': %s",
                            module.getName(),
                            openLMessage.getSummary());
                        messages.add(new OpenLMessage(message, Severity.ERROR));
                    }
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(classLoader);
                    try {
                        compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                }
                Summary summary = executeTests(compiledOpenClass);
                runTests += summary.getRunTests();
                failedTests += summary.getFailedTests();
                errors += summary.getErrors();
                summaryFailures.addAll(summary.getSummaryFailures());
                summaryErrors.addAll(summary.getSummaryErrors());
                hasCompilationErrors |= summary.isHasCompilationErrors();
            } finally {
                queueToReset.add(dependency);
                while (queueToReset.size() > maxModulesInMemory) {
                    queueToReset.poll();
                }
                factory.getDependencyManager().resetOthers(queueToReset.toArray(new IDependency[0]));
            }
        }

        OpenClassUtil.releaseClassLoader(classLoader);

        return new Summary(runTests, failedTests, errors, summaryFailures, summaryErrors, hasCompilationErrors);
    }

    private Summary executeTests(CompiledOpenClass openLRules) {
        TestRunner testRunner = getTestRunner();

        IOpenClass openClass = openLRules.getOpenClassWithErrors();

        if (openLRules.hasErrors()) {
            error("");
            error("There are compilation errors. It can affect test execution.");
            Collection<OpenLMessage> errorMessages = OpenLMessagesUtils
                .filterMessagesBySeverity(openLRules.getAllMessages(), Severity.ERROR);
            int i = 0;
            for (OpenLMessage message : errorMessages) {
                String location = message.getSourceLocation() == null ? "" : (" at " + message.getSourceLocation());
                error(i + 1 + ". '", message.getSummary(), "'", location);
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
                    String moduleInfo = moduleName == null ? "" : String.format(" from module '%s'", moduleName);
                    info("Running ", String.format("'%s'", test.getName()), moduleInfo, "...");
                    TestUnitsResults result;
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(openLRules.getClassLoader());
                        if (testSuiteExecutor == null) {
                            result = new TestSuite(test, testRunner).invokeSequentially(openClass, 1);
                        } else {
                            result = new TestSuite(test, testRunner).invokeParallel(testSuiteExecutor, openClass, 1);
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                    writeReport(result);

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

    private TestRunner getTestRunner() {
        if (testRunner == null) {
            TestRunner runner = new TestRunner(BaseTestUnit.Builder.getInstance());

            for (ReportFormat reportFormat : reportsFormat) {
                // For now xlsx exporter needs all info.
                if (reportFormat == ReportFormat.xlsx) {
                    runner = new TestRunner(TestUnit.Builder.getInstance());
                    break;
                }
            }

            testRunner = runner;
        }

        return testRunner;
    }

    private void writeReport(TestUnitsResults result) throws Exception {
        for (ReportFormat reporter : reportsFormat) {
            reporter.write(reportsDirectory, result);
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

                info("  Test case: #",
                    num,
                    ITestUnit.DEFAULT_DESCRIPTION.equals(description) ? "" : (" (" + description + ")"),
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
                            if (comparisonResult.getFieldName() == null || ThisField.THIS
                                .equals(comparisonResult.getFieldName())) {
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
                    throw new IllegalArgumentException(String.format("Incorrect thread count '%s'", threadCount));
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
