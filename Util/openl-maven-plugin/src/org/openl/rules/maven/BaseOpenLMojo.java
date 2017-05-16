package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
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
     * @deprecated Use sourceDirectory instead.
     */
    @Deprecated
    @Parameter
    private String openlResourcesDirectory;

    String getSourceDirectory() {
        File source;
        if (openlResourcesDirectory != null) {
            getLog().warn("<openlResourcesDirectory> parameter has been deprecated. Use <sourceDirectory> instead");
            source = new File(openlResourcesDirectory);
        } else {
            source = sourceDirectory;
        }
        String path;
        try {
            path = source.getCanonicalPath();
        } catch (Exception e) {
            warn("The path to OpenL source directory cannot be converted in canonical form.");
            path = source.getPath();
        }
        info("OpenL source directory: ", path);
        return path;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isDisabled()) {
            return;
        }
        info(SEPARATOR);
        info(getHeader());
        info(SEPARATOR);
        try {
            String openlRoot = getSourceDirectory();
            execute(openlRoot);
        } catch (MojoFailureException ex) {
            throw ex; // skip
        } catch (Exception ex) {
            throw new MojoFailureException("Execution failure.", ex);
        } finally {
            info(SEPARATOR);
        }
    }

    abstract void execute(String sourcePath) throws Exception;

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

    void warn(CharSequence message, Object... args) {
        if (getLog().isWarnEnabled()) {
            getLog().warn(getMessage(message, args));
        }
    }

    void error(CharSequence message, Object... args) {
        if (getLog().isErrorEnabled()) {
            getLog().error(getMessage(message, args));
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
