package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.util.CollectionUtils;

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
        ClassLoader classLoader;
        try {
            classLoader = composeClassLoader();
        } catch (Exception e) {
            throw new MojoFailureException("Failed to compose the classloader.", e);
        }

        String message = null;
        try {
            String openlRoot = getSourceDirectory().getCanonicalPath();
            info("Compiling the OpenL project from " + openlRoot);

            SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactoryBuilder<Object>();
            SimpleProjectEngineFactory<?> factory = builder.setProject(openlRoot)
                .setClassLoader(classLoader)
                .setExecutionMode(false)
                .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            info(SEPARATOR);
            info(getHeader());
            info(SEPARATOR);
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

    ClassLoader composeClassLoader() throws Exception {
        return null;
    }

    abstract String execute(CompiledOpenClass openLRules) throws Exception;

    boolean isDisabled() {
        return false;
    }

    String getHeader() {
        return getClass().getSimpleName();
    }

    URL[] toURLs(List<String> files) throws MalformedURLException {
        debug("Converting file paths to URLs...");
        ArrayList<URL> urls = new ArrayList<URL>(files.size());
        for (String file : files) {
            debug("   > ", file);
            urls.add(new File(file).toURI().toURL());
        }
        return urls.toArray(new URL[0]);
    }

    void info(CharSequence message, Object... args) {
        if (getLog().isInfoEnabled()) {
            getLog().info(getMessage(message, args));
        }
    }

    void error(Exception ex) {
        if (getLog().isErrorEnabled()) {
            getLog().error(ex);
        }
    }

    void debug(CharSequence message, Object... args) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(getMessage(message, args));
        }
    }

    private CharSequence getMessage(CharSequence message, Object[] args) {
        if (CollectionUtils.isEmpty(args)) {
            return message;
        }
        StringBuilder sb = new StringBuilder(message);
        for (Object obj : args) {
            sb.append(obj);
        }
        return sb;
    }
}
