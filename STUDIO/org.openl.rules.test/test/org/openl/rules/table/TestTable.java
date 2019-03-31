package org.openl.rules.table;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;

public class TestTable {
    @Test
    public void canRetrieveTestSuiteForIncorrectFieldArrayAccess() throws Exception {
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder
            .setExecutionMode(false)
            .setProject("test-resources/org/openl/rules/table")
            .setModule("EPBDS-7145")
            .build();
        IOpenClass openClass = simpleProjectEngineFactory.getCompiledOpenClass().getOpenClassWithErrors();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertNotNull(tests);
        assertEquals(0, tests.length);

        TestSuiteMethod hiTest = (TestSuiteMethod) openClass.getMethod("hiTest", new IOpenClass[0]);
        assertNotNull(hiTest);
        assertTrue(hiTest.isRunmethodTestable());
    }
}
