package main.java.org.openl.rules.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class BaseOpenLMojo extends AbstractMojo {

    /**
     * Folder that contains all OpenL-related resources (OpenL rules,
     * project descriptor etc.). For example: "${project.basedir}/src/main/openl".
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/openl")
    protected String openlResourcesDirectory;

    /**
     * Folder used by OpenL to compile rules. For example: ${project.build.directory}/openl".
     */
    @Parameter(defaultValue = "${project.build.directory}/openl")
    protected String openlOutputDirectory;

    @Component
    protected MavenProject project;

}
