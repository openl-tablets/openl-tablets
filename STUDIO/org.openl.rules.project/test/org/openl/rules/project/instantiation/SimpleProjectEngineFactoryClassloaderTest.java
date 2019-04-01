package org.openl.rules.project.instantiation;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class SimpleProjectEngineFactoryClassloaderTest {

    @Test
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/single1")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test(expected = org.openl.rules.project.instantiation.RulesInstantiationException.class)
    public void singleModuleWithoutLibsTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/single2")
            .build();
        factory.newInstance();
    }

    @Test
    public void classloader_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/single1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProject("test-resources/classpath/single2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_context_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/single1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideRuntimeContext(true)
            .setProject("test-resources/classpath/single2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_context_variation_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/single1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideRuntimeContext(true)
            .setProvideVariations(true)
            .setProject("test-resources/classpath/single2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void classloader_variation_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/single1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideVariations(true)
            .setProject("test-resources/classpath/single2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void multiModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/multi1")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test(expected = org.openl.rules.project.instantiation.RulesInstantiationException.class)
    public void multiModuleWithoutLibsTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/classpath/multi2")
            .build();
        factory.newInstance();
    }

    @Test
    public void multi_classloader_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/multi1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls);
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProject("test-resources/classpath/multi2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void multi_classloader_context_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/multi1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls);
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideRuntimeContext(true)
            .setProject("test-resources/classpath/multi2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void multi_classloader_context_variation_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/multi1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls);
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideRuntimeContext(true)
            .setProvideVariations(true)
            .setProject("test-resources/classpath/multi2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void multi_classloader_variation_Test() throws Exception {
        URL[] urls = { new URL("file:test-resources/classpath/multi1/beans.jar") };
        URLClassLoader classLoader = new URLClassLoader(urls);
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
            .setClassLoader(classLoader)
            .setProvideVariations(true)
            .setProject("test-resources/classpath/multi2")
            .build();
        Object instance = factory.newInstance();
        Assert.assertNotNull(instance);
    }
}
