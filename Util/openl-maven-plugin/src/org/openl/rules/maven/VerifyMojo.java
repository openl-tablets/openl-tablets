package org.openl.rules.maven;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Verifies if resulted archive is compatible with the OpenL Tablets Rules Engine
 *
 * @author Vladyslav Pikus
 * @since 5.24.0
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VerifyMojo extends BaseOpenLMojo {

    /**
     * Parameter to skip running OpenL Tablets verify goal if it set to 'true'.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    /**
     * Parameter to skip running OpenL Tablets verify goal if it set to 'true'.
     *
     * @deprecated for troubleshooting purposes
     */
    @Parameter(property = "skipITs")
    @Deprecated
    private boolean skipITs;

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true, required = true)
    private List<Artifact> pluginArtifacts;

    @Component
    private MavenSession mavenSession;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession session;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {
        var pathDeployment = project.getAttachedArtifacts()
            .stream()
            .filter(artifact -> PackageMojo.DEPLOYMENT_CLASSIFIER.equals(artifact.getClassifier()))
            .findFirst()
            .orElseGet(project::getArtifact)
            .getFile()
            .getPath();

        var openlJars = new HashSet<File>();

        // OpenL RuleServices Application dependencies
        openlJars.addAll(getJars("org.openl.rules:org.openl.rules.ruleservice.ws"));

        // Transitive dependencies required to be added to the same classpath of RuleServices
        openlJars.addAll(getTransitiveDependencies());

        // Dependencies from the plugin section
        for (var dep : plugin.getPlugin().getDependencies()) {
            openlJars.addAll(getJars(ArtifactUtils.versionlessKey(dep.getGroupId(), dep.getArtifactId())));
        }

        var jettyJars = new HashSet<URL>();

        // Jetty Server with annotations
        for (var x : getJars("org.eclipse.jetty:jetty-annotations")) {
            jettyJars.add(x.toURI().toURL());
        }

        // Enable logging in the Jetty server
        for (var x : getJars("org.slf4j:slf4j-simple")) {
            jettyJars.add(x.toURI().toURL());
        }

        // Required to provide runner AppServer class
        jettyJars.add(plugin.getPluginArtifact().getFile().toURI().toURL());

        // Instantiate and run Jetty server on clean classloader without Maven libraries
        var oldClassloader = Thread.currentThread().getContextClassLoader();
        try (var jettyClassLoader = new URLClassLoader(jettyJars.toArray(new URL[0]))) {
            Thread.currentThread().setContextClassLoader(jettyClassLoader);

            var appClass = jettyClassLoader.loadClass("org.openl.rules.maven.AppServer");
            var checkMethod = appClass.getDeclaredMethod("check", String.class, Set.class, String.class);
            checkMethod.invoke(null, pathDeployment, openlJars, outputDirectory.getPath());

            info(String.format("Verification is passed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()));
        } catch (Exception e) {
            throw new MojoFailureException(String
                    .format("Verification is failed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()), e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Gets path to the resolved jars, including transitive.
     * @param artifactId - groupId:artifactId
     * @return a set of downloaded jars
     * @throws DependencyResolutionException
     */
    private Set<File> getJars(String artifactId) throws DependencyResolutionException {
        // Find an artifact inside the openl-maven-plugin
        var artifact = pluginArtifacts.stream()
            .filter(x -> ArtifactUtils.versionlessKey(x).equals(artifactId))
            .map(RepositoryUtils::toArtifact)
            .findFirst().get();

        // Resolve transitive dependencies and get its jar files
        var collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
        var dependencyRequest = new DependencyRequest(collectRequest, null);
        var openlArtifacts = repositorySystem.resolveDependencies(session, dependencyRequest).getArtifactResults();


        return openlArtifacts.stream().map(x -> x.getArtifact().getFile()).collect(Collectors.toSet());
    }

    private Set<File> getTransitiveDependencies() {
        Set<String> pluginDependencies = plugin.getDependencies().stream()
                .map(d -> ArtifactUtils.versionlessKey(d.getGroupId(), d.getArtifactId()))
                .collect(Collectors.toSet());
        Set<String> allowedDependencies = getAllowedDependencies();
        return getDependentNonOpenLProjects().stream().filter(artifact -> {
            if (isOpenLCoreDependency(artifact.getGroupId())) {
                debug("SKIP : ", artifact);
                return false;
            }
            return true;
        }).filter(artifact -> {
            List<String> dependencyTrail = artifact.getDependencyTrail();
            if (dependencyTrail.size() < 2) {
                debug("SKIP : ", artifact, " (by dependency depth)");
                return false; // skip, unexpected size of dependencies
            }
            if (skipOpenLCoreDependency(dependencyTrail)) {
                debug("SKIP : ", artifact, " (transitive dependency from OpenL or SLF4j dependencies)");
                return false;
            }
            return true;
        }).filter(artifact -> !pluginDependencies.contains(ArtifactUtils.versionlessKey(artifact)))
          .filter(artifact -> {
            String tr = artifact.getDependencyTrail().get(1);
            String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
            return allowedDependencies.contains(key);
        }).map(Artifact::getFile).collect(Collectors.toSet());
    }

    private Set<String> getAllowedDependencies() {
        return project.getDependencies().stream().filter(dep -> {
            if (skipToProcess(dep.getScope(), dep.getGroupId())) {
                debug("SKIP : ", dep);
                return false;
            }
            return true;
        }).map(dep -> ArtifactUtils.versionlessKey(dep.getGroupId(), dep.getArtifactId())).collect(Collectors.toSet());
    }

    private static boolean skipToProcess(String scope, String group) {
        return !Artifact.SCOPE_PROVIDED.equals(scope) || isOpenLCoreDependency(group);
    }

    @Override
    boolean isDisabled() {
        return skipTests || skipITs;
    }

    @Override
    String getHeader() {
        return "OPENL VERIFY";
    }
}
