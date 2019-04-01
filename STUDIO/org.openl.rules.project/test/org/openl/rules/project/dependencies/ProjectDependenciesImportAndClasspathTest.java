package org.openl.rules.project.dependencies;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

public class ProjectDependenciesImportAndClasspathTest {
    private static final String SRC = "test-resources/dependencies/testImports/project2";
    private static final String SRC_WORKSPACE = "test-resources/dependencies/testImports";

    @Test
    public void test() throws Exception {
        ProjectEngineFactory<Object> projectEngineFactory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
            .setProject(SRC)
            .setExecutionMode(false)
            .setWorkspace(SRC_WORKSPACE)
            .setProvideRuntimeContext(true)
            .build();
        Assert.assertNotNull(projectEngineFactory);
        Assert.assertFalse(projectEngineFactory.getCompiledOpenClass().hasErrors());
    }
}
