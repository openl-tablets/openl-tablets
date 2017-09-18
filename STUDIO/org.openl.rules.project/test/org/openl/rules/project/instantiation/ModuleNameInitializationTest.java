package org.openl.rules.project.instantiation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;

public class ModuleNameInitializationTest {

    @Test
    public void moduleNameInTestSuite() throws Exception {
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject("test-resources/multi-module-support/module-name/")
                .setExecutionMode(false)
                .build();
        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertEquals(2, tests.length);

        for (TestSuiteMethod test : tests) {
            assertNotNull("Module name must be initialized", test.getModuleName());
            if (test.getName().startsWith("failed")) {
                assertEquals("Tests with Error", test.getModuleName());
            } else {
                assertEquals("Simple Rules", test.getModuleName());
            }
        }
    }

}
