package org.openl.rules.project.instantiation;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class SimpleProjectEngineFactoryClassloaderTest {

    @Test
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/project1")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test(expected = org.openl.rules.project.instantiation.RulesInstantiationException.class)
    public void singleModuleWithoutDepTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/project2")
            .build();
        factory.newInstance();
    }

    @Test
    public void classloader_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setWorkspace("test-resources/classpath")
            .setProject("test-resources/classpath/project2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_context_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProvideRuntimeContext(true)
            .setWorkspace("test-resources/classpath")
            .setProject("test-resources/classpath/project2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_context_variation_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProvideRuntimeContext(true)
            .setProvideVariations(true)
            .setWorkspace("test-resources/classpath")
            .setProject("test-resources/classpath/project2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_variation_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProvideVariations(true)
            .setWorkspace("test-resources/classpath")
            .setProject("test-resources/classpath/project2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

}
