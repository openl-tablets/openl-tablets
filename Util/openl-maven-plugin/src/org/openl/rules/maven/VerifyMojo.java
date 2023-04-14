package org.openl.rules.maven;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.codehaus.plexus.classworlds.ClassWorld;
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

    @Parameter( defaultValue = "${session}", readonly = true )
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

        var openlJars = new HashMap<String, File>();

        // OpenL RuleServices Application dependencies
        openlJars.putAll(getJars("org.openl.rules:org.openl.rules.ruleservice.ws"));

        // Transitive dependencies required to be added to the same classpath of RuleServices
        openlJars.putAll(getTransitiveDependencies());

        // Dependencies from the plugin section
        for (var dep : plugin.getPlugin().getDependencies()) {
            openlJars.putAll(getJars(ArtifactUtils.versionlessKey(dep.getGroupId(), dep.getArtifactId())));
        }

        // Remove log4j due LOG4J2-3657 and needeless to log to the file.
        openlJars.remove("org.apache.logging.log4j:log4j-core");
        openlJars.remove("org.apache.logging.log4j:log4j-slf4j-impl");

        var world = new ClassWorld();
        var jettyClassLoader = world.newRealm("jetty");

        // JettysourcePathsourcePath Server with annotations
        for (var x : getJars("org.eclipse.jetty:jetty-annotations").values()) {
            jettyClassLoader.addURL(x.toURI().toURL());
        }

        // Enable logging in the Jetty server via Maven logger API
        jettyClassLoader.importFrom(plugin.getClassRealm(), "org.slf4j");

        // Required to provide runner AppServer class
        jettyClassLoader.addURL(plugin.getPluginArtifact().getFile().toURI().toURL());

        // Instantiate and run Jetty server on clean classloader without Maven libraries
        var oldClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jettyClassLoader);

            var appClass = jettyClassLoader.loadClass("org.openl.rules.maven.AppServer");
            var checkMethod = appClass.getDeclaredMethod("check", String.class, Collection.class, String.class);
            checkMethod.invoke(null, pathDeployment, openlJars.values(), outputDirectory.getPath());

            info(String.format("Verification is passed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()));
        } catch (Exception e) {
            throw new MojoFailureException(String
                    .format("Verification is failed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()), e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
            world.disposeRealm("jetty");
        }
    }

    /**
     * Gets path to the resolved jars, including transitive.
     * @param artifactId - groupId:artifactId
     * @return a set of downloaded jars
     * @throws DependencyResolutionException
     */
    private Map<String, File> getJars(String artifactId) throws DependencyResolutionException {
        // Find an artifact inside the openl-maven-plugin
        var artifact = pluginArtifacts.stream()
            .filter(x -> ArtifactUtils.versionlessKey(x).equals(artifactId))
            .map(RepositoryUtils::toArtifact)
            .findFirst().get();

        // Resolve transitive dependencies and get its jar files
        var collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
        var dependencyRequest = new DependencyRequest(collectRequest, null);
        var openlDependencies = repositorySystem.resolveDependencies(session, dependencyRequest).getArtifactResults();

        var result = new HashMap<String, File>(openlDependencies.size());
        for (var x : openlDependencies) {
            var a = x.getArtifact();
            result.put(ArtifactUtils.versionlessKey(a.getGroupId(), a.getArtifactId()), a.getFile());
        }

        return result;
    }

    private Map<String, File> getTransitiveDependencies() {
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
        }).collect(Collectors.toMap(d -> ArtifactUtils.versionlessKey(d.getGroupId(), d.getArtifactId()), Artifact::getFile));
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
