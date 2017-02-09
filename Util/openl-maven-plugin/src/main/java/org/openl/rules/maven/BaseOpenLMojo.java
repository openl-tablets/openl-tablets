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

    /**
     * Folder used by OpenL to compile rules. For example:
     * ${project.build.directory}/openl".
     */
    @Parameter(defaultValue = "${project.build.directory}/openl")
    String openlOutputDirectory;

    @Component
    protected MavenProject project;

    File getSourceDirectory() {
        if (openlResourcesDirectory != null) {
            return new File(openlResourcesDirectory);
        }
        return sourceDirectory;
    }

    CompiledOpenClass compileOpenLRules() throws Exception {
        String openlRoot = getSourceDirectory().getPath();

        SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(openlRoot).build();

        return factory.getCompiledOpenClass();
    }
}
