package org.openl.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.test.RulesInFolderTestRunner;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class OpenLTest {

    public static final String DIR = "test-resources/functionality/";
    public static final String FAILURES_DIR = "test-resources/expected-test-failures/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @BeforeEach
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
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
            IOpenMethod method = openClass.getMethod("HelloTest12", IOpenClass.EMPTY);
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Main", testSuiteMethod.getModuleName(), "Module name must be initialized");
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[]{}, new SimpleRulesVM().getRuntimeEnv());
            assertTrue(result instanceof TestUnitsResults);
            TestUnitsResults testUnitsResults = (TestUnitsResults) result;
            assertEquals("HelloTest12()", testUnitsResults.getName(), "Unexpected test name");
            assertTrue(testUnitsResults.getExecutionTime() > 0, "Unexpected execution time");
            assertTrue(testUnitsResults.hasContext(), "Should have a context");
            assertEquals(19, testUnitsResults.getNumberOfTestUnits(), "Unexpected number of test cases");
            assertEquals(11, testUnitsResults.getNumberOfFailures(), "Unexpected number of failures");
            assertEquals(1, testUnitsResults.getNumberOfErrors(), "Unexpected number of errors");
            assertEquals(10, testUnitsResults.getNumberOfAssertionFailures(), "Unexpected number of assertions");
        }

        {
            IOpenMethod method = openClass.getMethod("GreetingTest", IOpenClass.EMPTY);
            assertNotNull(method);
            assertTrue(method instanceof TestSuiteMethod);
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
            assertEquals("Second Module", testSuiteMethod.getModuleName(), "Module name must be initialized");
            Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
            Object result = testSuiteMethod.invoke(instance, new Object[]{}, new SimpleRulesVM().getRuntimeEnv());
            assertTrue(result instanceof TestUnitsResults);
            TestUnitsResults testUnitsResults = (TestUnitsResults) result;
            assertEquals("GreetingTest()", testUnitsResults.getName(), "Unexpected test name");
            assertTrue(testUnitsResults.getExecutionTime() > 0, "Unexpected execution time");
            assertFalse(testUnitsResults.hasContext(), "Should not have a context");
            assertEquals(4, testUnitsResults.getNumberOfTestUnits(), "Unexpected number of test cases");
            assertEquals(0, testUnitsResults.getNumberOfFailures(), "Unexpected number of failures");
            assertEquals(0, testUnitsResults.getNumberOfErrors(), "Unexpected number of errors");
            assertEquals(0, testUnitsResults.getNumberOfAssertionFailures(), "Unexpected number of assertions");
        }

    }

    @Test
    public void testAllFailures() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(true, false);
        assertFalse(rulesInFolderTestRunner.run(FAILURES_DIR), "Test is failed.");
    }

    @Test
    public void testAll() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(false, false);
        assertFalse(rulesInFolderTestRunner.run(DIR), "Test is failed.");
    }

    @Test
    public void testAllInExecutionMode() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(false, true);
        assertFalse(rulesInFolderTestRunner.run(DIR), "Test is failed.");
    }
}
