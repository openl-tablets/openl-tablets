package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
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
                .build();
        CompiledOpenClass compiledOpenClass = simpleProjectEngineFactory.getCompiledOpenClass();
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertNotNull(tests);
        assertEquals(1, tests.length);
        assertEquals(1, compiledOpenClass.getAllMessages().size());
        assertEquals("Field '$Value$no_such_field' is not found.",
                compiledOpenClass.getAllMessages().iterator().next().getSummary());

        TestSuiteMethod hiTest = (TestSuiteMethod) openClass.getMethod("hiTest", IOpenClass.EMPTY);
        assertNotNull(hiTest);
        assertTrue(hiTest.isRunmethodTestable());
    }
}
