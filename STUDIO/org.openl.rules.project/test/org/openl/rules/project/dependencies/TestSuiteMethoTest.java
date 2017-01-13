package org.openl.rules.project.dependencies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class TestSuiteMethoTest {

    private static final String PROJECT_LOCATION = "test-resources/dependencies/test5/project1";
    private static final String WORKSPACE_LOCATION = "test-resources/dependencies/test5";

    SimpleProjectEngineFactory<Object> simpleProjectEngineFactory;

    @Before
    public void before() {
        SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactoryBuilder<Object>();
        simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder.setExecutionMode(false)
            .setProvideRuntimeContext(false)
            .setWorkspace(WORKSPACE_LOCATION)
            .setProject(PROJECT_LOCATION)
            .build();
    }

    @Test
    public void test() throws Exception {
        IOpenClass openClass = simpleProjectEngineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = openClass.getMethod("HelloTest12TestAll", new IOpenClass[] {});
        Assert.assertNotNull(method);
        Assert.assertTrue(method instanceof TestSuiteMethod);
        TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;
        Object instance = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
        Object result = testSuiteMethod.invoke(instance, new Object[] {}, new SimpleRulesVM().getRuntimeEnv());
        Assert.assertTrue(result instanceof TestUnitsResults);
        TestUnitsResults testUnitsResults = (TestUnitsResults) result;
        Assert.assertEquals(0, testUnitsResults.getNumberOfFailures());
    }
}
