package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.util.CollectionUtils;
import org.openl.util.ZipUtils;

abstract class BaseOpenLMojo extends AbstractMojo {
    private static final String SEPARATOR = "--------------------------------------------------";
    private static final Collection<String> OPENL_FILES = Arrays.asList(ProjectDescriptor.FILE_NAME, RulesDeploy.FILE_NAME);

    /**
     * Folder that contains all OpenL Tablets-related resources such as rules and project descriptor, for example,
     * ${project.basedir}/src/main/openl.
     *
     * @since 5.19.0
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../openl")
    private File sourceDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    /**
     * Directory containing the generated artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}/openl-workspace", required = true)
    protected File workspaceFolder;

    /**
     * The descriptor of this very plugin, injected by Maven. Used to discover the plugin's own GAV
     * (group/artifact/version) without hardcoding it — handy for both the OpenL-project detection in
     * {@link #isDisabled()} and for subclasses that need {@link PluginDescriptor#getVersion()} to embed
     * the plugin's version into their output (e.g. SCM commit messages in MigrateMojo).
     * <p>
     * It is effectively non-null field.
     * </p>
     */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    protected PluginDescriptor plugin;

    String getSourceDirectory() {
        String path;
        try {
            path = sourceDirectory.getCanonicalPath();
        } catch (Exception e) {
            warn("The path to OpenL source directory cannot be converted in canonical form.");
            path = sourceDirectory.getPath();
        }
        info("OpenL source directory: ", path);
        if (!sourceDirectory.isDirectory() || CollectionUtils.isEmpty(sourceDirectory.list())) {
            warn("OpenL source directory is empty.");
        }
        return path;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isDisabled()) {
            return;
        }
        if (!isOpenLProject(project.getBuildPlugins(), sourceDirectory)) {
            info("This module is not an OpenL project. Skipping.");
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

    /**
     * OpenL-project detector. Package-private so unit tests can drive both branches by passing a
     * {@code buildPlugins} list and a source directory directly. The plugin's own GAV is read from
     * the injected {@link #plugin} {@link PluginDescriptor}, so the plugin's coordinates are never
     * hardcoded.
     */
    boolean isOpenLProject(List<Plugin> buildPlugins, File sourceDirectory) {
        if (sourceDirectory != null && new File(sourceDirectory, ProjectDescriptor.FILE_NAME).isFile()) {
            return true;
        }
        if (buildPlugins == null) {
            return false;
        }
        var pluginGroupId = plugin.getGroupId();
        var pluginArtifactId = plugin.getArtifactId();
        return buildPlugins.stream().anyMatch(p ->
                pluginGroupId.equals(p.getGroupId()) && pluginArtifactId.equals(p.getArtifactId()));
    }

    String getHeader() {
        return getClass().getSimpleName();
    }

    URL[] toURLs(List<String> files) throws MalformedURLException {
        debug("Converting file paths to URLs...");
        ArrayList<URL> urls = new ArrayList<>(files.size());
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

    void debug(Exception ex) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(ex);
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

    protected Set<Artifact> getDependentOpenLProjects() {
        Set<Artifact> dependencies = new HashSet<>();
        for (Artifact artifact : project.getArtifacts()) {
            File file = artifact.getFile();
            if (ZipUtils.contains(file, OPENL_FILES::contains)) {
                dependencies.add(artifact);
                debug("OpenL artifact : ", artifact);
            }
        }
        return dependencies;
    }

    protected Set<Artifact> getDependentNonOpenLProjects() {
        Set<Artifact> dependencies = new HashSet<>();
        for (Artifact artifact : project.getArtifacts()) {
            File file = artifact.getFile();
            if (!ZipUtils.contains(file, OPENL_FILES::contains)) {
                dependencies.add(artifact);
                debug("Non OpenL artifact : ", artifact);
            }
        }
        return dependencies;
    }

    static boolean skipOpenLCoreDependency(List<String> dependencyTrail) {
        for (int i = 1; i < dependencyTrail.size() - 1; i++) {
            String dependency = dependencyTrail.get(i);
            if (dependency.startsWith("org.openl.rules:") || dependency.startsWith("org.openl:") || dependency
                    .startsWith("org.slf4j:")) {
                return true;
            }
        }
        return false;
    }

    static boolean isOpenLCoreDependency(String group) {
        return "org.openl.rules".equals(group) || "org.openl".equals(group) || "org.slf4j".equals(group);
    }

    static boolean isRuntimeScope(String scope) {
        return Artifact.SCOPE_RUNTIME.equals(scope) || Artifact.SCOPE_COMPILE.equals(scope);
    }

    /**
     * Returns the set of non-OpenL artifacts that the project declares (directly or transitively
     * through a declared dependency) and whose scope is accepted by {@code scopeFilter}. Mirrors the
     * filter used by {@code openl:package} (runtime/compile) and {@code openl:verify} (provided):
     * <ol>
     *     <li>artifact's scope must satisfy {@code scopeFilter} and must not be an OpenL-core
     *         dependency;</li>
     *     <li>artifact's dependency trail must contain at least the project entry plus one declared
     *         entry, and must not pass through an OpenL/SLF4j dependency;</li>
     *     <li>the {@code groupId:artifactId} key derived from the trail's second entry must be in
     *         the set of POM-declared dependencies that also satisfy {@code scopeFilter}.</li>
     * </ol>
     */
    protected Set<Artifact> getFilteredDependencies(Predicate<String> scopeFilter) {
        Set<String> allowed = getAllowedDependencies(scopeFilter);
        Set<Artifact> dependencies = new HashSet<>();
        for (Artifact artifact : getDependentNonOpenLProjects()) {
            if (!scopeFilter.test(artifact.getScope()) || isOpenLCoreDependency(artifact.getGroupId())) {
                debug("SKIP : ", artifact);
                continue;
            }
            List<String> dependencyTrail = artifact.getDependencyTrail();
            if (dependencyTrail.size() < 2) {
                debug("SKIP : ", artifact, " (by dependency depth)");
                continue;
            }
            if (skipOpenLCoreDependency(dependencyTrail)) {
                debug("SKIP : ", artifact, " (transitive dependency from OpenL or SLF4j dependencies)");
                continue;
            }
            String tr = dependencyTrail.get(1);
            String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
            if (allowed.contains(key)) {
                debug("ADD : ", artifact);
                dependencies.add(artifact);
            }
        }
        return dependencies;
    }

    /**
     * Returns the {@code groupId:artifactId} versionless keys of every POM-declared dependency whose
     * scope is accepted by {@code scopeFilter} and that is not an OpenL-core dependency. Used as the
     * "allowed" set in {@link #getFilteredDependencies(Predicate)}.
     */
    protected Set<String> getAllowedDependencies(Predicate<String> scopeFilter) {
        Set<String> allowed = new HashSet<>();
        for (Dependency dep : project.getDependencies()) {
            // Maven's effective model preserves the raw POM scope; the documented "compile" default
            // is applied later by the artifact resolver, not the model — normalize here.
            var scope = dep.getScope() == null ? Artifact.SCOPE_COMPILE : dep.getScope();
            if (!scopeFilter.test(scope) || isOpenLCoreDependency(dep.getGroupId())) {
                debug("SKIP : ", dep);
                continue;
            }
            allowed.add(ArtifactUtils.versionlessKey(dep.getGroupId(), dep.getArtifactId()));
        }
        return allowed;
    }

}
