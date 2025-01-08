package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

public class ProjectDependenciesImportAndClasspathTest {
    private static final String SRC = "test-resources/dependencies/testImports/project2";
    private static final String SRC_WORKSPACE = "test-resources/dependencies/testImports";

    @Test
    public void test() throws Exception {
        var projectEngineFactory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(SRC)
                .setExecutionMode(false)
                .setWorkspace(SRC_WORKSPACE)
                .setProvideRuntimeContext(true)
                .build();
        assertNotNull(projectEngineFactory);
        assertFalse(projectEngineFactory.getCompiledOpenClass().hasErrors());
    }
}
