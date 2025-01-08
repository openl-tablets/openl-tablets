package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class SimpleProjectEngineFactoryClassloaderTest {

    @Test
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/classpath/project1")
                .build();
        Object instance = factory.newInstance();
        assertNotNull(instance);
    }

    @Test
    public void singleModuleWithoutDepTest() throws Exception {
        assertThrows(RulesInstantiationException.class, () -> {
            SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                    .setProject("test-resources/classpath/project2")
                    .build();
            factory.newInstance();
        });
    }

    @Test
    public void classloader_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                .setWorkspace("test-resources/classpath")
                .setProject("test-resources/classpath/project2")
                .build();
        Object instance = factory.newInstance();
        assertNotNull(instance);
    }
}
