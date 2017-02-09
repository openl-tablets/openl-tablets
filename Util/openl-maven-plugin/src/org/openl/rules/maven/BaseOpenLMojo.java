package org.openl.rules.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

abstract class BaseOpenLMojo extends AbstractMojo {
    private static final String SEPARATOR = "--------------------------------------------------";

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isDisabled()) {
            return;
        }
        CompiledOpenClass openLRules;
        try {
            String openlRoot = getSourceDirectory().getPath();
            info("Compiling the OpenL project from " + openlRoot);

            SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactoryBuilder<Object>();
            SimpleProjectEngineFactory<?> factory = builder.setProject(openlRoot).setExecutionMode(false).build();

            openLRules = factory.getCompiledOpenClass();
        } catch (Exception e) {
            throw new MojoFailureException("Failed to compile OpenL project", e);
        }
        info(SEPARATOR);
        info(getHeader());
        info(SEPARATOR);
        String message = null;
        try {
            message = execute(openLRules);
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Execution failure.", e);
        } finally {
            info(SEPARATOR);
        }
        if (message != null) {
            throw new MojoExecutionException(message);
        }
    }

    abstract String execute(CompiledOpenClass openLRules) throws Exception;


    boolean isDisabled() {
        return false;
    }

    String getHeader() {
        return getClass().getSimpleName();
    }

    void info(String message) {
        if (getLog().isInfoEnabled()) {
            getLog().info(message);
        }
    }

    void error(Exception ex) {
        if (getLog().isErrorEnabled()) {
            getLog().error(ex);
        }
    }
}
