package org.openl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
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
            assertEquals("Incorrect test name", "HelloTest12()", testUnitsResults.getName());
            assertTrue("Incorrect execution time", testUnitsResults.getExecutionTime() > 0);
            assertTrue("Shoud have a context", testUnitsResults.hasContext());
            assertEquals("Incorrect count of test cases", 19, testUnitsResults.getNumberOfTestUnits());
            assertEquals("Incorrect count of failures", 11, testUnitsResults.getNumberOfFailures());
            assertEquals("Incorrect count of errors", 1, testUnitsResults.getNumberOfErrors());
            assertEquals("Incorrect count of assertions", 10, testUnitsResults.getNumberOfAssertionFailures());
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
            assertEquals("Incorrect test name", "GreetingTest()", testUnitsResults.getName());
            assertTrue("Incorrect execution time", testUnitsResults.getExecutionTime() > 0);
            assertFalse("Shoud not have a context", testUnitsResults.hasContext());
            assertEquals("Incorrect count of test cases", 4, testUnitsResults.getNumberOfTestUnits());
            assertEquals("Incorrect count of failures", 0, testUnitsResults.getNumberOfFailures());
            assertEquals("Incorrect count of errors", 0, testUnitsResults.getNumberOfErrors());
            assertEquals("Incorrect count of assertions", 0, testUnitsResults.getNumberOfAssertionFailures());
        }

    }

    @Test
    public void testAllFailuresExcelFiles() {
        testAllExcelFilesInFolder(FAILURES_DIR, false);
    }

    @Test
    public void testAllExcelFiles() {
        while (true) {
            testAllExcelFilesInFolder(DIR, true);
        }
    }

    private void testAllExcelFilesInFolder(String folderName, boolean pass) {
        LOG.info(">>> Running all OpenL tests...");
        boolean hasErrors = false;
        final File sourceDir = new File(folderName);

        if (!sourceDir.exists()) {
            LOG.warn("Tests directory doesn't exist!");
            return;
        }

        File[] files = sourceDir.listFiles();
//        files = new File[] {new File(sourceDir, "Arithmetic.xlsx")}; // Just for debugging.

        for (File file : files) {
            int errors = 0;
            String sourceFile = file.getName();
            CompiledOpenClass compiledOpenClass = null;
            if (file.isFile() && (sourceFile.endsWith(".xlsx") || sourceFile.endsWith(".xls"))) {
                try {
                    new FileInputStream(file).close();
                } catch (Exception ex) {
                    error(errors++, sourceFile, "Failed to read file.", ex);
                    hasErrors = true;
                    continue;
                }

                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(folderName + sourceFile);
                engineFactory.setExecutionMode(false);
                compiledOpenClass = engineFactory.getCompiledOpenClass();
            } else if (file.isDirectory()) {
                SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
                engineFactoryBuilder.setExecutionMode(false);
                engineFactoryBuilder.setProject(file.getPath());
                SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();

                try {
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                } catch (ClassNotFoundException | ProjectResolvingException | RulesInstantiationException e) {
                    error(errors++, sourceFile, "Compilation fails.", e);
                    hasErrors = true;
                    continue;
                }
            } else {
                // Skip not project files
                continue;
            }

            boolean success = true;

            // Check messages
            if (pass) {
                File msgFile = new File(sourceDir, sourceFile + ".msg.txt");
                List<String> expectedMessages = new ArrayList<>();

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
                        error(errors++, sourceFile, "Cannot read a file {}", msgFile, exc);
                    }

                    Collection<OpenLMessage> unexpectedMessages = new LinkedHashSet<>();
                    List<String> restMessages = new ArrayList<>(expectedMessages.size());
                    restMessages.addAll(expectedMessages);
                    for (OpenLMessage msg : compiledOpenClass.getMessages()) {
                        String actual = msg.getSeverity() + ": " + msg.getSummary();
                        if (msg.getSeverity().equals(Severity.ERROR) || msg.getSeverity().equals(Severity.FATAL)) {
                            success = false;
                        }
                        Iterator<String> itr = restMessages.iterator();
                        boolean found = false;
                        while (itr.hasNext()) {
                            if (actual.startsWith(itr.next())) {
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
                        error(errors++, sourceFile, "  UNEXPECTED messages:");
                        for (OpenLMessage msg : unexpectedMessages) {
                            error(errors++,
                                sourceFile,
                                "   {}: {}    at {}",
                                msg.getSeverity(),
                                msg.getSummary(),
                                msg.getSourceLocation());
                        }
                    }
                    if (!restMessages.isEmpty()) {
                        success = false;
                        error(errors++, sourceFile, "  MISSED messages:");
                        for (String msg : restMessages) {
                            error(errors++, sourceFile, "   {}", msg);
                        }
                    }
                }
            }

            // Check compilation
            if (success && compiledOpenClass.hasErrors()) {
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
                    error(errors++,
                        sourceFile,
                        "   {}: {}    at {}",
                        msg.getSeverity(),
                        msg.getSummary(),
                        msg.getSourceLocation());
                }
                success = false;
            }

            // Run tests
            if (success) {
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
                                    sourceFile,
                                    "Failed test: {}  Errors #: {}",
                                    res.getName(),
                                    numberOfFailures);
                            }
                        } else {
                            if (numberOfFailures != res.getNumberOfTestUnits()) {
                                error(errors++,
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
                LOG.info("OK in [{}].", sourceFile);
            }
        }

        assertFalse("Some tests have been failed!", hasErrors);
    }

    private void error(int count, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            LOG.error("ERROR in [{}].", sourceFile);
        }
        LOG.error(msg, args);
    }
}
