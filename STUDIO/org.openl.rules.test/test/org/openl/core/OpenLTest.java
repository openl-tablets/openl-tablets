package org.openl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
            IOpenMethod method = openClass.getMethod("HelloTest12", new IOpenClass[]{});
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Module name must be initialized", "Main", testSuiteMethod.getModuleName());
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[]{}, new SimpleRulesVM().getRuntimeEnv());
            assertTrue(result instanceof TestUnitsResults);
            TestUnitsResults testUnitsResults = (TestUnitsResults) result;
            assertEquals("Incorrect test name", "HelloTest12()", testUnitsResults.getName());
            assertTrue("Incorrect execution time", testUnitsResults.getExecutionTime() > 0);
            assertTrue("Shoud have a context", testUnitsResults.hasContext());
            assertEquals("Incorrect count of test cases", 19, testUnitsResults.getNumberOfTestUnits());
            assertEquals("Incorrect count of failures", 10, testUnitsResults.getNumberOfFailures());
            assertEquals("Incorrect count of errors", 1, testUnitsResults.getNumberOfErrors());
            assertEquals("Incorrect count of assertions", 9, testUnitsResults.getNumberOfAssertionFailures());
        }

        {
            IOpenMethod method = openClass.getMethod("GreetingTest", new IOpenClass[]{});
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Module name must be initialized", "Second Module", testSuiteMethod.getModuleName());
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[]{}, new SimpleRulesVM().getRuntimeEnv());
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
    public void testAllExcelFiles() throws NoSuchMethodException {
        LOG.info(">>> Running all OpenL tests...");
        boolean hasErrors = false;
        final File sourceDir = new File(DIR);

        if (!sourceDir.exists()) {
            LOG.warn("Tests directory doesn't exist!");
            return;
        }

        for (File file : sourceDir.listFiles()) {
            String sourceFile = file.getName();
            CompiledOpenClass compiledOpenClass = null;
            if (file.isFile() && (sourceFile.endsWith(".xlsx") || sourceFile.endsWith(".xls"))) {
                try {
                    new FileInputStream(file).close();
                } catch (Exception ex) {
                    LOG.error("Failed to read file [" + sourceFile + "]", ex);
                    hasErrors = true;
                    continue;
                }

                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(DIR + sourceFile);
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
                    LOG.error("Compilation fails for [" + sourceFile + "].", e);
                    hasErrors = true;
                    continue;
                }
            } else {
                // Skip not project files
                continue;
            }

            if (anyMessageFileExists(sourceDir, sourceFile)) {
                List<OpenLMessage> actualMessages = compiledOpenClass.getMessages();
                boolean hasAllMessages = true;
                for (Severity severity : Severity.values()) {
                    if (!assertMessages(sourceDir, sourceFile, actualMessages, severity)) {
                        hasAllMessages = false;
                    }
                }

                if (hasAllMessages) {
                    LOG.info("OK in [" + sourceFile + "].");
                } else {
                    hasErrors = true;
                }
            } else {
                if (compiledOpenClass.hasErrors()) {
                    LOG.error("Compilation errors in [" + sourceFile + "].");
                    LOG.error(compiledOpenClass.getMessages().toString());

                    hasErrors = true;
                    continue;
                }

                IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
                IOpenClass openClass = compiledOpenClass.getOpenClass();
                Object target = openClass.newInstance(env);
                int errors = 0;
                for (IOpenMethod method : openClass.getDeclaredMethods()) {
                    if (method instanceof TestSuiteMethod) {
                        TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                        final int numberOfFailures = res.getNumberOfFailures();
                        errors += numberOfFailures;
                        if (numberOfFailures != 0) {
                            LOG.error("Errors in [" + sourceFile + "]. Failed test: " + res.getName() + "  Errors #:" + numberOfFailures);
                        }

                    }
                }
                if (errors != 0) {
                    hasErrors = true;
                    LOG.error("Errors in [" + sourceFile + "]. Total failures #: " + errors);
                } else {
                    LOG.info("OK in [" + sourceFile + "]. ");
                }
            }
        }

        assertFalse("Some tests have been failed!", hasErrors);
    }

    private boolean anyMessageFileExists(File sourceDir, String projectFile) {
        for (Severity severity : Severity.values()) {
            File messagesFile = new File(sourceDir, projectFile + "." + severity.name().toLowerCase() + ".txt");
            if (messagesFile.exists()) {
                return true;
            }
        }

        return false;
    }

    private boolean assertMessages(File sourceDir,
                                   String projectFile,
                                   List<OpenLMessage> actualMessages,
                                   Severity severity) {
        boolean hasAllMessages = true;

        File file = new File(sourceDir, projectFile + "." + severity.name().toLowerCase() + ".txt");
        try {
            for (String expectedMessage : getExpectedMessages(file)) {
                if (!findExpectedMessage(actualMessages, expectedMessage, severity)) {
                    LOG.error("The message \"" + expectedMessage + "\" with severity " + severity
                            .name() + " is missed for [" + projectFile + "].");
                    hasAllMessages = false;
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to read file [" + file + "].", e);
            hasAllMessages = false;
        }

        if (!hasAllMessages) {
            StringBuilder sb = new StringBuilder();
            sb.append("Actual messages:");
            for (OpenLMessage openLMessage : actualMessages) {
                sb.append(System.lineSeparator());
                sb.append("\t\"" + openLMessage.getSummary() + "\" with severity " + openLMessage.getSeverity() + ".");
            }
            LOG.error(sb.toString());
        }

        return hasAllMessages;
    }

    private List<String> getExpectedMessages(File file) throws IOException {
        List<String> result = new ArrayList<>();

        if (!file.exists()) {
            return result;
        }

        String content = IOUtils.toStringAndClose(new FileInputStream(file));
        for (String message : content.split("\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]")) {
            if (!StringUtils.isBlank(message)) {
                result.add(message.trim());
            }
        }

        return result;
    }

    private boolean findExpectedMessage(List<OpenLMessage> actualMessages, String expectedMessage, Severity severity) {
        for (OpenLMessage message : actualMessages) {
            if (message.getSummary().equals(expectedMessage) && message.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }
}
