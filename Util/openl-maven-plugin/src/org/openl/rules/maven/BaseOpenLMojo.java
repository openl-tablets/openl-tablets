package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.openl.util.CollectionUtils;
import org.openl.util.ZipUtils;

abstract class BaseOpenLMojo extends AbstractMojo {
    private static final String SEPARATOR = "--------------------------------------------------";
    protected static final String OPENL_ARTIFACT_TYPE = "zip";

    /**
     * Folder that contains all OpenL-related resources (OpenL rules, project
     * descriptor etc.). For example: "${project.basedir}/src/main/openl".
     *
     * @since 5.19.0
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../openl")
    private File sourceDirectory;

    /**
     * @deprecated Use sourceDirectory instead.
     */
    @Deprecated
    @Parameter
    private String openlResourcesDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    /**
     * Directory containing the generated artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}/openl-workspace", required = true)
    protected File workspaceFolder;

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
        if (!source.isDirectory() || CollectionUtils.isEmpty(source.list())) {
            warn("OpenL source directory is empty.");
        }
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
            Collection<Artifact> dependencies = getDependentOpenLProjects();
            boolean hasDependencies = !dependencies.isEmpty();
            if (hasDependencies) {
                debug("Has ", dependencies.size(), " dependencies");
                for (Artifact artifact : dependencies) {
                    debug("Extract dependency ", artifact.getArtifactId());
                    File projectFolder = new File(workspaceFolder, artifact.getArtifactId());
                    if (!projectFolder.exists()) {
                        ZipUtils.extractAll(artifact.getFile(), projectFolder);
                    }
                }
            }
            String openlRoot = getSourceDirectory();
            execute(openlRoot, hasDependencies);
        } catch (MojoFailureException ex) {
            throw ex; // skip
        } catch (Exception ex) {
            throw new MojoFailureException("Execution failure.", ex);
        } finally {
            info(SEPARATOR);
        }
    }

    abstract void execute(String sourcePath, boolean hasDependencies) throws Exception;

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

    private Collection<Artifact> getDependentOpenLProjects() {
        List<Artifact> dependencies = new ArrayList<>();
        for (Artifact artifact : project.getArtifacts()) {
            if (OPENL_ARTIFACT_TYPE.equals(artifact.getType())) {
                dependencies.add(artifact);
            }
        }

        return dependencies;
    }

}
