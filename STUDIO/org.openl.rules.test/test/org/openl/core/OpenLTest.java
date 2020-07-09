package org.openl.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenLTest {

    public static final String DIR = "test-resources/functionality/";
    public static final String FAILURES_DIR = "test-resources/expected-test-failures/";
    private static final Logger LOG = LoggerFactory.getLogger(OpenLTest.class);
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void checkTestBehavior() throws Exception {
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder
            .setExecutionMode(false)
            .setProject("test-resources/check-openl-test")
            .build();
        IOpenClass openClass = simpleProjectEngineFactory.getCompiledOpenClass().getOpenClass();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertEquals(2, tests.length);
        {
            IOpenMethod method = openClass.getMethod("HelloTest12", new IOpenClass[] {});
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Module name must be initialized", "Main", testSuiteMethod.getModuleName());
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[] {}, new SimpleRulesVM().getRuntimeEnv());
            assertTrue(result instanceof TestUnitsResults);
            TestUnitsResults testUnitsResults = (TestUnitsResults) result;
            assertEquals("Unexpected test name", "HelloTest12()", testUnitsResults.getName());
            assertTrue("Unexpected execution time", testUnitsResults.getExecutionTime() > 0);
            assertTrue("Should have a context", testUnitsResults.hasContext());
            assertEquals("Unexpected number of test cases", 19, testUnitsResults.getNumberOfTestUnits());
            assertEquals("Unexpected number of failures", 11, testUnitsResults.getNumberOfFailures());
            assertEquals("Unexpected number of errors", 1, testUnitsResults.getNumberOfErrors());
            assertEquals("Unexpected number of assertions", 10, testUnitsResults.getNumberOfAssertionFailures());
        }

        {
            IOpenMethod method = openClass.getMethod("GreetingTest", new IOpenClass[] {});
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Module name must be initialized", "Second Module", testSuiteMethod.getModuleName());
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[] {}, new SimpleRulesVM().getRuntimeEnv());
            assertTrue(result instanceof TestUnitsResults);
            TestUnitsResults testUnitsResults = (TestUnitsResults) result;
            assertEquals("Unexpected test name", "GreetingTest()", testUnitsResults.getName());
            assertTrue("Unexpected execution time", testUnitsResults.getExecutionTime() > 0);
            assertFalse("Should not have a context", testUnitsResults.hasContext());
            assertEquals("Unexpected number of test cases", 4, testUnitsResults.getNumberOfTestUnits());
            assertEquals("Unexpected number of failures", 0, testUnitsResults.getNumberOfFailures());
            assertEquals("Unexpected number of errors", 0, testUnitsResults.getNumberOfErrors());
            assertEquals("Unexpected number of assertions", 0, testUnitsResults.getNumberOfAssertionFailures());
        }

    }

    @Test
    public void testAllFailuresExcelFiles() {
        testAllExcelFilesInFolder(FAILURES_DIR, false, false);
    }

    @Test
    public void testAllExcelFiles() {
        testAllExcelFilesInFolder(DIR, true, false);
    }

    @Test
    public void testAllExcelFilesInExecutionMode() {
        testAllExcelFilesInFolder(DIR, true, true);
    }

    private void testAllExcelFilesInFolder(String testsDirPath, boolean pass, boolean executionMode) {
        if (executionMode) {
            LOG.info(">>> Compiling rules from directory '{}' in execution mode...", testsDirPath);
        } else {
            LOG.info(">>> Compiling and running tests from directory '{}'...", testsDirPath);
        }
        boolean hasErrors = false;
        final File testsDir = new File(testsDirPath);

        if (!testsDir.exists()) {
            LOG.warn("Tests folder is not found.");
            return;
        }

        File[] files = testsDir.listFiles();
        // files = new File[] {new File(sourceDir, "CastsTest.xlsx")}; // Just for debugging.
        if (files == null) {
            return;
        }
        for (File file : files) {
            final long startTime = System.nanoTime();
            int errors = 0;
            String sourceFile = file.getName();
            CompiledOpenClass compiledOpenClass = null;
            if (file.isFile() && (sourceFile.endsWith(".xlsx") || sourceFile.endsWith(".xls"))) {
                try {
                    new FileInputStream(file).close();
                } catch (Exception ex) {
                    error(errors++, startTime, sourceFile, "Failed to read excel file.", ex);
                    hasErrors = true;
                    continue;
                }

                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(testsDirPath + sourceFile);
                engineFactory.setExecutionMode(executionMode);
                compiledOpenClass = engineFactory.getCompiledOpenClass();
            } else if (file.isDirectory()) {
                SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
                engineFactoryBuilder.setExecutionMode(executionMode);
                engineFactoryBuilder.setProject(file.getPath());
                SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();

                try {
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                } catch (ProjectResolvingException | RulesInstantiationException e) {
                    error(errors++, startTime, sourceFile, "Compilation fails.", e);
                    hasErrors = true;
                    continue;
                }
            } else {
                // Skip not project files
                continue;
            }

            boolean success = true;

            // Check messages
            File msgFile = new File(testsDir, sourceFile + ".msg.txt");
            List<String> expectedMessages = new ArrayList<>();
            if (msgFile.exists() && executionMode) {
                continue;
            }
            if (msgFile.exists()) {
                try {
                    String content = IOUtils.toStringAndClose(new FileInputStream(msgFile));
                    for (String message : content
                        .split("\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]")) {
                        if (!StringUtils.isBlank(message)) {
                            expectedMessages.add(message.trim());
                        }
                    }
                } catch (IOException exc) {
                    error(errors++, startTime, sourceFile, "Failed to read messages file.", msgFile, exc);
                }

                Collection<OpenLMessage> unexpectedMessages = new LinkedHashSet<>();
                List<String> restMessages = new ArrayList<>(expectedMessages);
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
                    String actual = msg.getSeverity() + ": " + msg.getSummary();
                    if (msg.getSeverity().equals(Severity.ERROR) || msg.getSeverity().equals(Severity.FATAL)) {
                        success = false;
                    }
                    Iterator<String> itr = restMessages.iterator();
                    boolean found = false;
                    while (itr.hasNext()) {
                        if (actual.contains(itr.next())) {
                            itr.remove();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        unexpectedMessages.add(msg);
                    }
                }
                if (!unexpectedMessages.isEmpty()) {
                    success = false;
                    error(errors++, startTime, sourceFile, "  UNEXPECTED messages:");
                    for (OpenLMessage msg : unexpectedMessages) {
                        error(errors++,
                            startTime,
                            sourceFile,
                            "   {}: {}    at {}",
                            msg.getSeverity(),
                            msg.getSummary(),
                            msg.getSourceLocation());
                    }
                }
                if (!restMessages.isEmpty()) {
                    success = false;
                    error(errors++, startTime, sourceFile, "  MISSED messages:");
                    for (String msg : restMessages) {
                        error(errors++, startTime, sourceFile, "   {}", msg);
                    }
                }
            }

            // Check compilation
            if (success && compiledOpenClass.hasErrors()) {
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
                    error(errors++,
                        startTime,
                        sourceFile,
                        "   {}: {}    at {}",
                        msg.getSeverity(),
                        msg.getSummary(),
                        msg.getSourceLocation());
                }
                success = false;
            }

            // Run tests
            if (success && !executionMode) {
                IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
                IOpenClass openClass = compiledOpenClass.getOpenClass();
                Object target = openClass.newInstance(env);
                for (IOpenMethod method : openClass.getDeclaredMethods()) {
                    if (method instanceof TestSuiteMethod) {
                        TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                        final int numberOfFailures = res.getNumberOfFailures();
                        if (pass) {
                            if (numberOfFailures != 0) {
                                error(errors++,
                                    startTime,
                                    sourceFile,
                                    "Failed test: {}  Errors #: {}",
                                    res.getName(),
                                    numberOfFailures);
                                List<ITestUnit> failed = res.getFilteredTestUnits(true, 3);
                                for (ITestUnit testcase : failed) {
                                    error(errors++,
                                        startTime,
                                        sourceFile,
                                        "\n   #{}  \n Actual: {} \n Expected: {}",
                                        testcase.getTest().getId(),
                                        testcase.getActualResult(),
                                        testcase.getExpectedResult());
                                }
                            }
                        } else {
                            if (numberOfFailures != res.getNumberOfTestUnits()) {
                                error(errors++,
                                    startTime,
                                    sourceFile,
                                    "Unexpected result test: {}  Errors #: {}",
                                    res.getName(),
                                    res.getNumberOfTestUnits() - numberOfFailures);
                            }
                        }
                    }
                }
            }

            // Output
            if (errors != 0) {
                hasErrors = true;
            } else {
                ok(startTime, executionMode, sourceFile);
            }
        }

        assertFalse("Some tests have been failed.", hasErrors);
    }

    private void ok(long startTime, boolean executionMode, String sourceFile) {
        final long ms = (System.nanoTime() - startTime) / 1000000;
        LOG.info("{} - in [{}] ({} ms)", executionMode ? "EXECUTION MODE COMPILED" : "SUCCESS", sourceFile, ms);
    }

    private void error(int count, long startTime, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            final long ms = (System.nanoTime() - startTime) / 1000000;
            LOG.error("FAILURE - in [{}] ({} ms)", sourceFile, ms);
        }
        LOG.error(msg, args);
    }
}
