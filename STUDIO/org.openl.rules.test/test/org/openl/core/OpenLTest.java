package org.openl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
            IOpenMethod method = openClass.getMethod("HelloTest12", IOpenClass.EMPTY);
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
            IOpenMethod method = openClass.getMethod("GreetingTest", IOpenClass.EMPTY);
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
    public void testAllFailures() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(true, false);
        assertFalse("Test is failed.", rulesInFolderTestRunner.run(FAILURES_DIR));
    }

    @Test
    public void testAll() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(false, false);
        assertFalse("Test is failed.", rulesInFolderTestRunner.run(DIR));
    }

    @Test
    public void testAllInExecutionMode() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(false, true);
        assertFalse("Test is failed.", rulesInFolderTestRunner.run(DIR));
    }
}
