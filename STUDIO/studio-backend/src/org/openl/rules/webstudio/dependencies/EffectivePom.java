package org.openl.rules.webstudio.dependencies;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.resolution.ModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jspecify.annotations.Nullable;

/**
 * The {@code pom.xml} of an OpenL project as Maven sees it: with what its parents give it, its properties put in,
 * and the versions its dependency management sets, those of the imported BOMs included.
 *
 * <p>The OpenL Maven Plugin packs the dependencies a {@code pom.xml} declares into the {@code lib} folder of the
 * project. A dependency of the groups OpenL Rule Services runs on, {@link #EMBEDDED_GROUPS}, is left out, and so is an
 * archive of another OpenL project, the {@code zip} of it. A {@code provided} dependency is not packed: the application
 * running the project, such as OpenL Rule Services, ships it.
 *
 * @author Yury Molchan
 */
final class EffectivePom {

    /** The groups OpenL Studio and OpenL Rule Services bring themselves, which no project takes as a library. */
    static final Set<String> EMBEDDED_GROUPS = Set.of("org.openl.rules", "org.openl", "org.slf4j");
    /** The scopes of the dependencies a project runs with. */
    private static final Set<String> RUNTIME_SCOPES = Set.of(JavaScopes.COMPILE, JavaScopes.RUNTIME);
    /** The system properties a model may read, such as to choose its profiles by the Java version. */
    private static final List<String> SYSTEM_PROPERTIES = List.of("java.version",
            "java.home",
            "os.name",
            "os.arch",
            "os.version",
            "file.separator",
            "path.separator",
            "line.separator");
    private static final ModelBuilder BUILDER = new DefaultModelBuilderFactory().newInstance();

    private final Model model;
    private final ArtifactTypeRegistry types;

    private EffectivePom(Model model, ArtifactTypeRegistry types) {
        this.model = model;
        this.types = types;
    }

    /**
     * Builds the model of the {@code pom.xml}.
     *
     * <p>A parent is read by its relative path beside the {@code pom.xml} first, as in a build of several modules,
     * then from the Maven repository; the imported BOMs are read from the Maven repository. The repositories it
     * declares are not read.
     *
     * @param pom      the {@code pom.xml}
     * @param resolver reads the POMs it refers to from the Maven repository
     * @param types    the types of artifacts, as Maven reads a dependency of a type
     * @throws ModelBuildingException when the model cannot be built, such as for a parent the repository lacks
     */
    static EffectivePom read(ProjectPom pom, ModelResolver resolver, ArtifactTypeRegistry types)
            throws ModelBuildingException {
        var request = new DefaultModelBuildingRequest().setModelSource(pom.modelSource())
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setProcessPlugins(false)
                .setTwoPhaseBuilding(false)
                .setSystemProperties(systemProperties())
                .setModelResolver(resolver);
        return new EffectivePom(BUILDER.build(request).getEffectiveModel(), types);
    }

    private static Properties systemProperties() {
        var properties = new Properties();
        SYSTEM_PROPERTIES.forEach(key -> {
            var value = System.getProperty(key);
            if (value != null) {
                properties.setProperty(key, value);
            }
        });
        return properties;
    }

    /**
     * The libraries the project runs with: its dependencies of the {@code compile} and {@code runtime} scopes, but the
     * archives of other OpenL projects and the libraries of {@link #EMBEDDED_GROUPS}.
     */
    List<Dependency> libraries() {
        return librariesIn(RUNTIME_SCOPES);
    }

    /**
     * The libraries the application running the project provides: its dependencies of the {@code provided} scope, but
     * the archives of other OpenL projects and the libraries of {@link #EMBEDDED_GROUPS}.
     */
    List<Dependency> provided() {
        return librariesIn(Set.of(JavaScopes.PROVIDED));
    }

    /** The libraries among the dependencies of the scopes. */
    private List<Dependency> librariesIn(Set<String> scopes) {
        return model.getDependencies()
                .stream()
                .filter(dependency -> scopes.contains(scopeOf(dependency)) && isKnown(dependency))
                .map(this::dependencyOf)
                .filter(dependency -> isLibrary(dependency.getArtifact()))
                .toList();
    }

    /**
     * The archives of the OpenL projects the project depends on: its {@code zip} dependencies of the {@code compile}
     * and {@code runtime} scopes, which the OpenL Maven Plugin unpacks into the workspace the project compiles in.
     */
    List<Dependency> projects() {
        return model.getDependencies()
                .stream()
                .filter(dependency -> RUNTIME_SCOPES.contains(scopeOf(dependency)))
                .map(this::dependencyOf)
                .filter(dependency -> isProject(dependency.getArtifact()))
                .toList();
    }

    /**
     * Whether Maven knows the type of the dependency without an extension of the build: a {@code tile}, for one, is
     * known only to the build that loads the extension, which reads it; it is no library.
     */
    private boolean isKnown(org.apache.maven.model.Dependency dependency) {
        return types.get(dependency.getType()) != null;
    }

    /** The versions the dependency management sets, of the dependencies and of what they need in turn. */
    List<Dependency> managed() {
        var management = model.getDependencyManagement();
        return management == null
                ? List.of()
                : management.getDependencies().stream().map(this::managedOf).toList();
    }

    /**
     * The artifact with the version the dependency management sets for its group and artifact, as the OpenL Maven
     * Plugin sets it for the coordinates {@code rules.xml} names.
     */
    Artifact managedVersionOf(Artifact artifact) {
        var management = model.getDependencyManagement();
        if (management == null) {
            return artifact;
        }
        return management.getDependencies()
                .stream()
                .filter(managed -> isOf(managed, artifact))
                .findFirst()
                .map(managed -> artifact.setVersion(managed.getVersion()))
                .orElse(artifact);
    }

    /** Whether the {@code pom.xml} declares a dependency of the group and artifact, which it then decides of. */
    boolean declares(Artifact artifact) {
        return model.getDependencies().stream().anyMatch(dependency -> isOf(dependency, artifact));
    }

    /** Whether the artifact is a library a project takes: neither an OpenL archive nor one OpenL brings itself. */
    static boolean isLibrary(Artifact artifact) {
        return !MavenDependencyResolver.ZIP.equals(artifact.getExtension())
                && !EMBEDDED_GROUPS.contains(artifact.getGroupId());
    }

    /** Whether the artifact is the archive of an OpenL project, as the OpenL Maven Plugin packages one. */
    static boolean isProject(Artifact artifact) {
        return MavenDependencyResolver.ZIP.equals(artifact.getExtension())
                && !EMBEDDED_GROUPS.contains(artifact.getGroupId());
    }

    private static boolean isOf(org.apache.maven.model.Dependency dependency, Artifact artifact) {
        return Objects.equals(dependency.getGroupId(), artifact.getGroupId())
                && Objects.equals(dependency.getArtifactId(), artifact.getArtifactId());
    }

    private static String scopeOf(org.apache.maven.model.Dependency dependency) {
        return dependency.getScope() == null ? JavaScopes.COMPILE : dependency.getScope();
    }

    /** The dependency as Maven Resolver reads it, with its type, its scope and what it excludes. */
    private Dependency dependencyOf(org.apache.maven.model.Dependency dependency) {
        return dependencyOf(dependency, scopeOf(dependency), dependency.isOptional());
    }

    /**
     * The managed dependency as Maven reads it: it sets only what it states, so a scope or an optional flag it leaves
     * out is left to the dependency, such as the scope a library of a {@code provided} one takes from it.
     */
    private Dependency managedOf(org.apache.maven.model.Dependency dependency) {
        return dependencyOf(dependency,
                dependency.getScope(),
                dependency.getOptional() == null ? null : dependency.isOptional());
    }

    private Dependency dependencyOf(org.apache.maven.model.Dependency dependency,
                                    @Nullable String scope,
                                    @Nullable Boolean optional) {
        var type = types.get(dependency.getType());
        if (type == null) {
            type = new DefaultArtifactType(dependency.getType());
        }
        var artifact = new DefaultArtifact(dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                type);
        var exclusions = dependency.getExclusions()
                .stream()
                .map(exclusion -> new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*"))
                .toList();
        return new Dependency(artifact, scope, optional, exclusions);
    }
}
