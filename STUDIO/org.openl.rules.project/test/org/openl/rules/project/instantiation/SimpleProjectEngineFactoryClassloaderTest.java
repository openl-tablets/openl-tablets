package org.openl.rules.project.instantiation;

import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

import java.net.URL;
import java.net.URLClassLoader;

public class SimpleProjectEngineFactoryClassloaderTest {

    @Test
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setProject("test-resources/classpath/single1").build();
        factory.newInstance();
    }

    @Test(expected = org.openl.rules.project.instantiation.RulesInstantiationException.class)
    public void singleModuleWithoutLibsTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setProject("test-resources/classpath/single2").build();
        factory.newInstance();
    }

    @Test
    public void singleModuleWithClassloaderTest() throws Exception {
        URL[] urls = {new URL("file:test-resources/classpath/single1/beans.jar")};
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setClassLoader(classLoader)
                .setProject("test-resources/classpath/single2").build();
        factory.newInstance();
    }

    @Test
    public void multiModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/classpath/multi1").build();
        factory.newInstance();
    }

    @Test(expected = org.openl.rules.project.instantiation.RulesInstantiationException.class)
    public void multiModuleWithoutLibsTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setProject("test-resources/classpath/multi2").build();
        factory.newInstance();
    }

    @Test
    public void multiModuleWithClassloaderTest() throws Exception {
        URL[] urls = {new URL("file:test-resources/classpath/multi1/beans.jar")};
        URLClassLoader classLoader = new URLClassLoader(urls);
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setClassLoader(classLoader)
                .setProject("test-resources/classpath/multi2").build();
        factory.newInstance();
    }
}
