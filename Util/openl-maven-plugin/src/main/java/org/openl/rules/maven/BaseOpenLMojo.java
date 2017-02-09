package org.openl.rules.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

abstract class BaseOpenLMojo extends AbstractMojo {

    /**
     * Folder that contains all OpenL-related resources (OpenL rules, project
     * descriptor etc.). For example: "${project.basedir}/src/main/openl".
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../openl")
    private File sourceDirectory;

    /**
     * Folder that contains all OpenL-related resources (OpenL rules, project
     * descriptor etc.).
     * 
     * @deprecated Use sourceDirectory instead.
     */
    @Deprecated
    @Parameter
    private String openlResourcesDirectory;

    @Component
    protected MavenProject project;

    File getSourceDirectory() {
        if (openlResourcesDirectory != null) {
            getLog().warn("<openlResourcesDirectory> parameter is deprecated. Use <sourceDirectory> instead");
            return new File(openlResourcesDirectory);
        }
        return sourceDirectory;
    }

    CompiledOpenClass compileOpenLRules() throws Exception {
        String openlRoot = getSourceDirectory().getPath();
        if (getLog().isInfoEnabled()) {
            getLog().info("Compiling the OpenL project from " + openlRoot);
        }

        SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(openlRoot).setExecutionMode(false).build();

        return factory.getCompiledOpenClass();
    }
}
