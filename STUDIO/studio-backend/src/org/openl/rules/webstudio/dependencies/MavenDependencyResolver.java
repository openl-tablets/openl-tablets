package org.openl.rules.webstudio.dependencies;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.Locked;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.studio.dependencies.service.MavenCoordinates;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

/**
 * Downloads from a Maven repository the dependencies a {@code rules.xml} names by Maven coordinates.
 *
 * <p>A dependency with no {@code <name>} is a library: it is downloaded with the libraries it needs at runtime, and
 * OpenL Studio puts them on the classpath of the project. The dependencies a {@code pom.xml} of the project declares
 * are libraries too, as the OpenL Maven Plugin packs them into the {@code lib} folder of the project; the
 * {@code pom.xml} decides of a library both name. Its {@code provided} dependencies are downloaded too, unless OpenL
 * Studio ships them itself: the application running the project provides them. A dependency with a {@code <name>} is
 * an OpenL project, named by its coordinates or by the {@code zip} of the {@code pom.xml} whose project has the name:
 * its archive is downloaded and unpacked, and OpenL Studio compiles it as a dependency when the workspace has no
 * project of that name.
 *
 * <p>The coordinates are written as {@code groupId:artifactId[:extension[:classifier]]:version}, as the OpenL Maven
 * Plugin reads them, so a coordinate without the extension names a {@code zip} archive.
 *
 * <p>Nothing is downloaded while no repository is set. What was found, or failed, is kept in memory for a few
 * minutes, so a project that compiles again does not ask the repository each time. The files are kept in a local
 * repository between restarts.
 *
 * @author Yury Molchan
 */
@Component
@Slf4j
public class MavenDependencyResolver implements DisposableBean {

    /** How long what was found, or failed, is kept in memory. */
    static final Duration CACHE_PERIOD = Duration.ofMinutes(5);

    /** The extension of the archive of an OpenL project. */
    public static final String ZIP = "zip";
    private static final String REPOSITORY_ID = "openl";
    static final String POM = "pom";
    /**
     * A part of coordinates: no path separator and no dot first, so it names no file outside the local repository,
     * and a group, mapped to folders, starts in none above it.
     */
    private static final Pattern COORDINATE_PART = Pattern.compile("[\\w+-][\\w.+-]*");
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int REQUEST_TIMEOUT_MILLIS = 60_000;
    /**
     * The filter that asks the repository only for the groups its {@code .meta/prefixes.txt} lists. A repository
     * manager grouping a proxy of Maven Central with hosted repositories may publish the list of the proxy alone, so
     * the filter would refuse every artifact of the company; Maven 3 does not use it either.
     */
    private static final String PREFIXES_FILTER = "aether.remoteRepositoryFilter.prefixes";
    /**
     * What a library brings at runtime, and a provided library OpenL Studio does not ship, but an archive of another
     * OpenL project or a library of the groups OpenL brings itself, and what either needs in turn.
     */
    private static final DependencyFilter LIBRARIES = DependencyFilterUtils.andFilter(
            new ScopeDependencyFilter(JavaScopes.TEST, JavaScopes.SYSTEM),
            MavenDependencyResolver::isToDownload);
    /** The archives of OpenL projects, and those they depend on in turn. */
    private static final DependencyFilter PROJECTS = DependencyFilterUtils.andFilter(
            DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME),
            (node, parents) -> isProject(node) && parents.stream().allMatch(MavenDependencyResolver::isProject));

    private final @Nullable RemoteRepository repository;
    private final Path localRepository;
    private final Clock clock;
    private final Map<Object, Cached<?>> cache = new ConcurrentHashMap<>();
    private final Object extractLock = new Object();
    /** Resolutions read the local repository together; a removal from it runs alone. */
    private final ReadWriteLock repositoryLock = new ReentrantReadWriteLock();
    private final LocalMavenRepository local;
    private final MissingArtifacts missing;
    /** The files read by what keeps them in use a while, released once it is gone. */
    private final Map<Object, Set<Path>> held = new WeakHashMap<>();
    /** Created at the first resolution, as no resolution may happen at all. */
    private final AtomicReference<@Nullable RepositorySystem> system = new AtomicReference<>();

    @Autowired
    public MavenDependencyResolver(@Value("${maven.repository.url:}") @Nullable String url,
                                   @Value("${maven.repository.username:}") @Nullable String username,
                                   @Value("${maven.repository.password:}") @Nullable String password,
                                   @Value("${maven.repository.local}") String localRepository) {
        this(url, username, password, Path.of(localRepository), Clock.systemUTC());
    }

    MavenDependencyResolver(@Nullable String url,
                            @Nullable String username,
                            @Nullable String password,
                            Path localRepository,
                            Clock clock) {
        this.repository = remoteRepository(url, username, password);
        // Maven Resolver tells the files it reads by absolute paths, so the folder is told so too
        this.localRepository = localRepository.toAbsolutePath().normalize();
        this.clock = clock;
        this.local = new LocalMavenRepository(this.localRepository, clock);
        this.missing = new MissingArtifacts(clock, CACHE_PERIOD);
    }

    private static @Nullable RemoteRepository remoteRepository(@Nullable String url,
                                                               @Nullable String username,
                                                               @Nullable String password) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        var builder = new RemoteRepository.Builder(REPOSITORY_ID, "default", url.trim())
                .setSnapshotPolicy(new RepositoryPolicy(true,
                        RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN));
        if (StringUtils.isNotBlank(username)) {
            builder.setAuthentication(new AuthenticationBuilder().addUsername(username)
                    .addPassword(password)
                    .build());
        }
        return builder.build();
    }

    /** Whether a Maven repository is set to download the dependencies from. */
    public boolean isEnabled() {
        return repository != null;
    }

    /**
     * Downloads the libraries the project names by Maven coordinates and those its {@code pom.xml} declares, with the
     * libraries they need at runtime.
     *
     * @param project the project
     * @param pom     the {@code pom.xml} of the project, or {@code null} when it has none or carries its
     *                libraries, as a project downloaded from the Maven repository does in its {@code lib} folder
     * @return the libraries found, and what went wrong; nothing while no repository is set
     */
    public Libraries librariesOf(ProjectDescriptor project, @Nullable ProjectPom pom) {
        return librariesNamed(libraryCoordinatesOf(project.getDependencies()), pom);
    }

    /**
     * Downloads the libraries the coordinates name and those the {@code pom.xml} declares, with the libraries they
     * need at runtime. The {@code pom.xml} is read as Maven reads it: with its parents, its properties and its
     * dependency management, which sets the version of the coordinates too.
     *
     * <p>Neither an archive of another OpenL project nor what it needs is a library, since that project is compiled
     * with its own; nor is a library of the groups OpenL brings itself, {@code org.openl.rules}, {@code org.openl} and
     * {@code org.slf4j}, nor what it needs. The OpenL Maven Plugin leaves them out of the {@code lib} folder too.
     *
     * <p>The {@code provided} dependencies of the {@code pom.xml}, with what they need, are told apart: the application
     * running the project ships them. One OpenL Studio ships itself, with what it needs, is not downloaded.
     *
     * @param coordinates the coordinates of the libraries, as the dependencies without a name write them
     * @param pom         the {@code pom.xml} of the project, or {@code null} when it has none
     * @return the libraries found, and what went wrong; nothing while no repository is set
     */
    public Libraries librariesNamed(List<String> coordinates, @Nullable ProjectPom pom) {
        return libraries(coordinates, pom, true);
    }

    /**
     * Downloads the libraries a deployment of the project carries: those of
     * {@link #librariesNamed(List, ProjectPom)}, but the {@code provided} ones, which the application running the
     * project ships. A {@code provided} library the Maven repository lacks does not stop the deployment.
     *
     * @param coordinates the coordinates of the libraries, as the dependencies without a name write them
     * @param pom         the {@code pom.xml} of the project, or {@code null} when it has none
     * @return the libraries found, and what went wrong; nothing while no repository is set
     */
    public Libraries librariesToDeploy(List<String> coordinates, @Nullable ProjectPom pom) {
        return libraries(coordinates, pom, false);
    }

    private Libraries libraries(List<String> coordinates, @Nullable ProjectPom pom, boolean withProvided) {
        var distinct = coordinates.stream().map(String::trim).distinct().toList();
        if (!isEnabled() || distinct.isEmpty() && pom == null) {
            return Libraries.NONE;
        }
        var key = new LibrariesKey(distinct, pom, withProvided);
        return cached(key, () -> resolveLibraries(key));
    }

    /**
     * Whether the folder holds a project downloaded from the Maven repository: one in the local repository, which
     * carries its libraries and names the projects it depends on in its {@code rules.xml}.
     */
    public boolean isDownloaded(Path folder) {
        return folder.toAbsolutePath().normalize().startsWith(localRepository);
    }
    /**
     * Downloads the OpenL project the dependency names by Maven coordinates.
     *
     * @param dependency a dependency with a name and Maven coordinates
     * @return the project, or what went wrong; nothing while no repository is set
     */
    public DownloadedProject projectOf(ProjectDependencyDescriptor dependency) {
        var coordinates = dependency.getMavenArtifact();
        if (!isEnabled() || coordinates == null || dependency.getName() == null) {
            return DownloadedProject.NONE;
        }
        return cached(new ProjectKey(dependency.getName(), coordinates),
                () -> resolveProject(dependency.getName(), coordinates));
    }

    /**
     * Downloads the OpenL project the Maven coordinates name, whatever its name: a project looked at by its
     * coordinates, which no dependency may name.
     *
     * @param coordinates the coordinates of the archive of the project, as a dependency writes them
     * @return the project, or what went wrong; nothing while no repository is set
     */
    public DownloadedProject projectAt(String coordinates) {
        if (!isEnabled()) {
            return DownloadedProject.NONE;
        }
        return cached(new ProjectKey(null, coordinates), () -> resolveProject(null, coordinates));
    }

    /**
     * The OpenL projects the {@code pom.xml} of a project depends on, as the OpenL Maven Plugin finds them: the
     * archives of its {@code zip} dependencies and those they depend on in turn, by the names their {@code rules.xml}
     * give. A dependency of the {@code rules.xml} of the project that names only a project is the project of that
     * name.
     *
     * <p>A pom-less project gives the projects its {@code rules.xml} names by Maven coordinates too, when the
     * dependency management of its anchor sets another version of them, as the plugin builds it with that version.
     *
     * <p>A project downloaded from the Maven repository has none to tell: the archive of the project that depends on
     * it named those it depends on in turn.
     *
     * @param pom the {@code pom.xml}, or {@code null} when the project has none
     * @return the coordinates of the projects by their names, and what went wrong; nothing while no repository is set
     */
    public PomProjects projectsOfPom(@Nullable ProjectPom pom) {
        if (!isEnabled() || pom == null) {
            return PomProjects.NONE;
        }
        return cached(new PomProjectsKey(pom), () -> resolvePomProjects(pom));
    }

    /**
     * The coordinates of the libraries among the dependencies: a dependency without a name is a library.
     *
     * @param dependencies the dependencies a {@code rules.xml} declares, or {@code null} when it declares none
     * @return the coordinates, in the order the dependencies declare them
     */
    public static List<String> libraryCoordinatesOf(@Nullable Collection<ProjectDependencyDescriptor> dependencies) {
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .filter(dependency -> dependency.getName() == null)
                .map(ProjectDependencyDescriptor::getMavenArtifact)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * The dependencies that name an OpenL project by Maven coordinates, those of the project of its name a
     * {@code pom.xml} depends on winning over those the dependency names.
     *
     * @param dependencies the dependencies a {@code rules.xml} declares, or {@code null} when it declares none
     * @param pomProjects  the coordinates of the projects the {@code pom.xml} depends on, by name
     * @return the dependencies, in the order they are declared
     * @see #projectsOfPom(ProjectPom)
     */
    public static List<ProjectDependencyDescriptor> namedMavenDependenciesOf(
            @Nullable Collection<ProjectDependencyDescriptor> dependencies,
            Map<String, String> pomProjects) {
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .filter(dependency -> dependency.getName() != null)
                .map(dependency -> withPomCoordinates(dependency, pomProjects))
                .filter(dependency -> dependency.getMavenArtifact() != null)
                .toList();
    }

    /**
     * The dependency with the coordinates of the project of its name a {@code pom.xml} depends on, which win over
     * those it names itself, as the {@code pom.xml} decides in the OpenL Maven Plugin.
     *
     * @param dependency  a dependency with a name
     * @param pomProjects the coordinates of the projects the {@code pom.xml} depends on, by name
     * @return the dependency, or a copy of it with the coordinates of the {@code pom.xml}
     */
    public static ProjectDependencyDescriptor withPomCoordinates(ProjectDependencyDescriptor dependency,
                                                                 Map<String, String> pomProjects) {
        var coordinates = pomProjects.get(dependency.getName());
        if (coordinates == null || coordinates.equals(dependency.getMavenArtifact())) {
            return dependency;
        }
        var named = new ProjectDependencyDescriptor();
        named.setName(dependency.getName());
        named.setMavenArtifact(coordinates);
        return named;
    }

    /**
     * Answers what is kept for the key, or resolves it again once the period has passed. A resolution runs once for
     * every caller asking for the same key meanwhile. What no project has asked for during the period, such as the
     * coordinates a {@code rules.xml} no longer names, is forgotten.
     */
    @SuppressWarnings("unchecked")
    <T> T cached(Object key, Supplier<T> resolution) {
        var now = clock.instant();
        cache.values().removeIf(cached -> cached.isExpired(now));
        var fresh = new Cached<T>(new CompletableFuture<>(), now.plus(CACHE_PERIOD));
        var current = (Cached<T>) cache.compute(key,
                (k, cached) -> cached == null || cached.isExpired(now) ? fresh : cached);
        if (current == fresh) {
            try {
                fresh.answer().complete(resolution.get());
            } catch (RuntimeException | Error e) {
                // Every caller waiting for the answer is told the failure, and the next one resolves again
                cache.remove(key, fresh);
                fresh.answer().completeExceptionally(e);
                throw e;
            }
        }
        return current.answer().join();
    }

    /** The size of the files kept in the local repository, in bytes. */
    public long localRepositorySize() {
        return local.size();
    }

    /**
     * Removes from the local repository the versions no project needs: those unused for longer than the period, then,
     * while the versions downloaded or used are larger than the limit, those used longest ago. What the answers kept in
     * memory name stays, since the projects compiled at the moment read it.
     *
     * @param unusedFor how long a version may stay unused, or {@code null} for no limit
     * @param maxBytes  how large the versions downloaded or used may grow, or {@code 0} for no limit
     * @return what was removed
     */
    @Locked.Write("repositoryLock")
    LocalMavenRepository.Removal cleanLocalRepository(@Nullable Duration unusedFor, long maxBytes) {
        return local.clean(unusedFor, maxBytes, filesInUse());
    }

    /**
     * Removes every file of the local repository and forgets every answer, so each project downloads its
     * dependencies again when it compiles next. A file the operating system keeps open, as Windows does for a jar a
     * class loader reads, stays.
     *
     * @return what was removed
     */
    @Locked.Write("repositoryLock")
    LocalMavenRepository.Removal clearLocalRepository() {
        cache.clear();
        missing.clear();
        return local.clear();
    }

    /**
     * Keeps the files from a cleanup of the local repository while the holder is in use: a dependency manager
     * reading the libraries and the downloaded projects it compiles, or a session reading the downloaded projects it
     * opened. They are released once nothing refers to the holder.
     *
     * @param holder what reads the files
     * @param files  the files, or the folders of the unpacked projects
     */
    public void hold(Object holder, Collection<Path> files) {
        if (!files.isEmpty()) {
            synchronized (held) {
                held.computeIfAbsent(holder, key -> new HashSet<>()).addAll(files);
            }
        }
    }

    /**
     * The files in use: those the answers kept in memory name, the libraries and the unpacked projects, and those
     * the dependency managers and the sessions hold.
     */
    private List<Path> filesInUse() {
        var files = new ArrayList<Path>();
        synchronized (held) {
            held.values().forEach(files::addAll);
        }
        for (Cached<?> cached : cache.values()) {
            switch (cached.answer().getNow(null)) {
                case Libraries libraries -> {
                    files.addAll(libraries.files());
                    files.addAll(libraries.provided());
                }
                case DownloadedProject downloaded when downloaded.project() != null ->
                        files.add(downloaded.project().getProjectFolder());
                case null, default -> {
                    // Nothing found, so nothing read
                }
            }
        }
        return files;
    }

    /** How many answers are kept in memory. */
    int cachedAnswers() {
        return cache.size();
    }

    /**
     * Resolves the libraries and, apart, the {@code provided} ones: what a {@code provided} library needs is read
     * with it, behind the project's own libraries, even when a library of the project needs it too.
     */
    @Locked.Read("repositoryLock")
    private Libraries resolveLibraries(LibrariesKey key) {
        var messages = new ArrayList<OpenLMessage>();
        var repositories = repositories();
        var libraries = new CollectRequest();
        libraries.setRepositories(repositories);
        var provided = new CollectRequest();
        provided.setRepositories(repositories);
        try (var session = newSession()) {
            var pom = key.pom() == null ? null : effectivePom(key.pom(), session, repositories, messages);
            if (pom != null) {
                pom.libraries().forEach(libraries::addDependency);
                libraries.setManagedDependencies(pom.managed());
                if (key.withProvided()) {
                    pom.provided().forEach(provided::addDependency);
                    provided.setManagedDependencies(pom.managed());
                }
            }
            for (String coordinate : key.coordinates()) {
                var artifact = artifactOf(coordinate);
                if (artifact == null) {
                    messages.add(error("The Maven coordinates '%s' are not valid.".formatted(coordinate)));
                } else if (pom == null) {
                    libraries.addDependency(new Dependency(artifact, JavaScopes.RUNTIME));
                } else if (!pom.declares(artifact)) {
                    libraries.addDependency(new Dependency(pom.managedVersionOf(artifact), JavaScopes.RUNTIME));
                }
            }
            return new Libraries(filesOf(session, libraries, messages), filesOf(session, provided, messages), messages);
        }
    }

    /** The files of the libraries the request names, with those they need; what cannot be downloaded is told. */
    private List<Path> filesOf(RepositorySystemSession session, CollectRequest collect, List<OpenLMessage> messages) {
        if (collect.getDependencies().isEmpty()) {
            return List.of();
        }
        var files = new ArrayList<Path>();
        for (ArtifactResult result : resolve(session, new DependencyRequest(collect, LIBRARIES), messages)) {
            if (!result.isResolved()) {
                result.getExceptions().forEach(e -> messages.add(downloadError(e)));
            } else if (isClasspathFile(result.getArtifact())) {
                files.add(result.getArtifact().getPath());
            }
        }
        return files;
    }

    /** Whether the node of a dependency graph is a library, or the root of the graph, which names no artifact. */
    private static boolean isLibrary(DependencyNode node) {
        return node.getArtifact() == null || EffectivePom.isLibrary(node.getArtifact());
    }

    private static boolean isProject(DependencyNode node) {
        return node.getArtifact() == null || EffectivePom.isProject(node.getArtifact());
    }

    private static boolean isToDownload(DependencyNode node, List<DependencyNode> parents) {
        if (!isLibrary(node) || !parents.stream().allMatch(MavenDependencyResolver::isLibrary)) {
            return false;
        }
        // A provided library OpenL Studio ships, or one such a library needs, is read from OpenL Studio
        return !isProvided(node) || !isShipped(node) && parents.stream().noneMatch(MavenDependencyResolver::isShipped);
    }

    private static boolean isProvided(DependencyNode node) {
        return node.getDependency() != null && JavaScopes.PROVIDED.equals(node.getDependency().getScope());
    }

    /**
     * Whether OpenL Studio ships the library itself: the Maven descriptor a jar of it holds tells, or, for a jar built
     * without one, such as a jar of Spring, a jar of OpenL Studio named after the artifact, {@code artifactId-version},
     * that holds classes of the group of the library.
     */
    private static boolean isShipped(DependencyNode node) {
        var artifact = node.getArtifact();
        if (artifact == null) {
            return false;
        }
        var library = artifact.getGroupId() + "/" + artifact.getArtifactId();
        return ShippedJars.SHIPPED.computeIfAbsent(library, key -> MavenDependencyResolver.class.getClassLoader()
                .getResource("META-INF/maven/%s/pom.properties".formatted(key)) != null
                || ShippedJars.hasJarOf(artifact.getGroupId(), artifact.getArtifactId()));
    }

    /** The jars of OpenL Studio by the artifacts they are named after, read once. */
    static final class ShippedJars {
        private static final Pattern JAR_NAME = Pattern.compile("(.+?)-\\d[^/]*\\.jar");
        private static final Map<String, List<Path>> JARS = jars();
        /** Whether OpenL Studio ships each library asked about, by its group and artifact. */
        static final Map<String, Boolean> SHIPPED = new ConcurrentHashMap<>();

        private ShippedJars() {
        }

        /**
         * Whether a jar named after the artifact holds classes of the group: a jar of another group may have the same
         * name, such as the {@code utils} of the AWS SDK. A group is matched by its package, or by its first two
         * names for a group whose classes sit in a package of the company, such as {@code org.springframework.boot}.
         */
        static boolean hasJarOf(String groupId, String artifactId) {
            return JARS.getOrDefault(artifactId, List.of()).stream().anyMatch(jar -> holdsClassesOf(jar, groupId));
        }

        /** Whether the jar holds a file in the package of the group, or in that of its first two names. */
        static boolean holdsClassesOf(Path jar, String groupId) {
            var names = groupId.split("\\.");
            var packages = names.length > 2
                    ? List.of(groupId.replace('.', '/') + "/", names[0] + "/" + names[1] + "/")
                    : List.of(groupId.replace('.', '/') + "/");
            try (var file = new JarFile(jar.toFile())) {
                return file.stream().anyMatch(entry -> packages.stream().anyMatch(entry.getName()::startsWith));
            } catch (IOException e) {
                return false;
            }
        }

        private static Map<String, List<Path>> jars() {
            var jars = new ArrayList<Path>();
            for (var entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
                if (!entry.isBlank()) {
                    jars.add(Path.of(entry));
                }
            }
            for (var loader = MavenDependencyResolver.class.getClassLoader(); loader != null;
                 loader = loader.getParent()) {
                if (loader instanceof URLClassLoader urls) {
                    Arrays.stream(urls.getURLs()).map(ShippedJars::pathOf).filter(Objects::nonNull).forEach(jars::add);
                }
            }
            var byArtifact = new HashMap<String, List<Path>>();
            for (var jar : jars) {
                var matcher = JAR_NAME.matcher(String.valueOf(jar.getFileName()));
                if (matcher.matches()) {
                    byArtifact.computeIfAbsent(matcher.group(1), key -> new ArrayList<>()).add(jar);
                }
            }
            return Map.copyOf(byArtifact);
        }

        private static @Nullable Path pathOf(URL url) {
            try {
                return "file".equals(url.getProtocol()) ? Path.of(url.toURI()) : null;
            } catch (URISyntaxException | IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** The repository the dependencies are downloaded from, as a resolution asks for it. */
    private List<RemoteRepository> repositories() {
        return List.of(Objects.requireNonNull(repository));
    }

    /** Whether the file goes on the classpath: a POM only names what a dependency of the type {@code pom} brings. */
    private static boolean isClasspathFile(Artifact artifact) {
        return !POM.equals(artifact.getExtension());
    }

    /** Reads the {@code pom.xml} as Maven does, or tells why it cannot be read. */
    private @Nullable EffectivePom effectivePom(ProjectPom pom,
                                                RepositorySystemSession session,
                                                List<RemoteRepository> repositories,
                                                List<OpenLMessage> messages) {
        try {
            return EffectivePom.read(pom,
                    new PomModelResolver(system(), session, repositories),
                    session.getArtifactTypeRegistry());
        } catch (ModelBuildingException e) {
            log.warn("Cannot read the pom.xml of a project.", e);
            messages.add(error("Cannot read the pom.xml of the project: " + e.getProblems()
                    .stream()
                    .filter(problem -> problem.getSeverity() != ModelProblem.Severity.WARNING)
                    .map(ModelProblem::getMessage)
                    .collect(Collectors.joining(" "))));
            return null;
        }
    }

    private List<ArtifactResult> resolve(RepositorySystemSession session,
                                         DependencyRequest request,
                                         List<OpenLMessage> messages) {
        try {
            return system().resolveDependencies(session, request).getArtifactResults();
        } catch (DependencyResolutionException e) {
            log.warn("Cannot download the Maven dependencies {}.", request.getCollectRequest().getDependencies(), e);
            var result = e.getResult();
            if (result.getArtifactResults().stream().allMatch(ArtifactResult::isResolved)) {
                messages.add(downloadError(e));
            }
            return result.getArtifactResults();
        }
    }

    @Locked.Read("repositoryLock")
    private PomProjects resolvePomProjects(ProjectPom pom) {
        var messages = new ArrayList<OpenLMessage>();
        var repositories = repositories();
        var collect = new CollectRequest();
        collect.setRepositories(repositories);
        List<ArtifactResult> results;
        var byName = new LinkedHashMap<String, String>();
        try (var session = newSession()) {
            var model = effectivePom(pom, session, repositories, messages);
            if (model == null) {
                return new PomProjects(Map.of(), messages);
            }
            byName.putAll(managedProjects(pom, model));
            if (model.projects().isEmpty()) {
                return new PomProjects(byName, messages);
            }
            // A module of the same build is built with it, not downloaded
            var root = PomReactor.rootOf(pom);
            var modules = root == null
                    ? Set.<String>of()
                    : cached(new ReactorKey(root.text(), root.path()), () -> PomReactor.modulesOf(root));
            model.projects()
                    .stream()
                    .filter(dependency -> !PomReactor.holds(modules, dependency.getArtifact()))
                    .forEach(collect::addDependency);
            if (collect.getDependencies().isEmpty()) {
                return new PomProjects(byName, messages);
            }
            collect.setManagedDependencies(model.managed());
            results = resolve(session, new DependencyRequest(collect, PROJECTS), messages);
        }
        for (ArtifactResult result : results) {
            if (result.isResolved()) {
                var coordinates = coordinatesOf(result.getArtifact());
                var project = projectAt(coordinates).project();
                // An archive that is no OpenL project is no project the rules.xml can name
                if (project != null && project.getName() != null) {
                    byName.putIfAbsent(project.getName(), coordinates);
                }
            } else {
                result.getExceptions().forEach(e -> messages.add(downloadError(e)));
            }
        }
        return new PomProjects(byName, messages);
    }

    /**
     * The coordinates of the OpenL projects the {@code rules.xml} of a pom-less project names, by their names, whose
     * versions the dependency management of its anchor sets, as the OpenL Maven Plugin sets them. A project the
     * management leaves alone keeps the coordinates of its dependency.
     */
    private static Map<String, String> managedProjects(ProjectPom pom, EffectivePom model) {
        var managed = new LinkedHashMap<String, String>();
        pom.projects().forEach((name, coordinates) -> {
            var artifact = artifactOf(coordinates);
            var version = artifact == null ? null : model.managedVersionOf(artifact).getVersion();
            if (version != null && !version.equals(artifact.getVersion())) {
                managed.put(name, coordinatesOf(artifact.setVersion(version)));
            }
        });
        return managed;
    }

    /**
     * The coordinates of the archive as {@code rules.xml} writes them: a plain {@code zip}, the packaging of an OpenL
     * project, without its extension, as a project is addressed by; another one with its extension and classifier.
     */
    private static String coordinatesOf(Artifact artifact) {
        return ZIP.equals(artifact.getExtension()) && artifact.getClassifier().isEmpty()
                ? "%s:%s:%s".formatted(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion())
                : ArtifactIdUtils.toBaseId(artifact);
    }

    /**
     * Downloads and reads the project. A project of a dependency must have the name of the dependency; a project
     * downloaded with no name to have may have any.
     */
    @Locked.Read("repositoryLock")
    private DownloadedProject resolveProject(@Nullable String name, String coordinates) {
        var ofDependency = name == null ? "" : " of the dependency '%s'".formatted(name);
        var artifact = artifactOf(coordinates);
        if (artifact == null) {
            return new DownloadedProject(null,
                    error("The Maven coordinates '%s'%s are not valid.".formatted(coordinates, ofDependency)));
        }
        Path archive;
        try (var session = newSession()) {
            var request = new ArtifactRequest(artifact, repositories(), null);
            archive = system().resolveArtifact(session, request).getArtifact().getPath();
        } catch (ArtifactResolutionException e) {
            log.warn("Cannot download the project{} from '{}'.", ofDependency, coordinates, e);
            return new DownloadedProject(null, downloadError(e));
        }
        try {
            var project = ProjectResolver.getInstance().resolve(unpacked(archive));
            if (project == null) {
                return new DownloadedProject(null,
                        error("'%s'%s is not an OpenL project.".formatted(coordinates, ofDependency)));
            }
            if (name != null && !name.equals(project.getName())) {
                return new DownloadedProject(null,
                        error("'%s' is the project '%s', not the dependency '%s'.".formatted(coordinates,
                                project.getName(),
                                name)));
            }
            return new DownloadedProject(project, null);
        } catch (IOException | ProjectResolvingException e) {
            log.warn("Cannot read the project{} downloaded from '{}'.", ofDependency, coordinates, e);
            return new DownloadedProject(null,
                    error("Cannot read the project%s downloaded from '%s': %s".formatted(ofDependency,
                            coordinates,
                            e.getMessage())));
        }
    }

    /**
     * The folder the archive is unpacked into, beside it. A new version of the archive, as a snapshot gets, is
     * unpacked into a folder of its own, so a class loader reading the previous one keeps its files.
     */
    private Path unpacked(Path archive) throws IOException {
        var name = archive.getFileName().toString();
        var baseName = name.endsWith("." + ZIP) ? name.substring(0, name.length() - ZIP.length() - 1) : name;
        var folder = archive.resolveSibling(baseName + "-" + Files.getLastModifiedTime(archive).toMillis());
        if (Files.isDirectory(folder)) {
            // Unpacked already: the folder appears whole, moved in once unpacked
            return folder;
        }
        synchronized (extractLock) {
            if (!Files.isDirectory(folder)) {
                var temp = Files.createTempDirectory(archive.getParent(), baseName + "-");
                try {
                    ZipUtils.extractAll(archive.toFile(), temp.toFile());
                    Files.move(temp, folder, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    if (!Files.isDirectory(folder)) {
                        throw e;
                    }
                    // Another instance sharing the folder unpacked it meanwhile
                    log.debug("The archive '{}' is unpacked already.", archive, e);
                } finally {
                    FileUtils.deleteQuietly(temp);
                }
            }
        }
        return folder;
    }

    /**
     * The coordinates of the downloaded project unpacked into the folder.
     *
     * <p>A project is unpacked beside its archive, in the folder of its version of the local repository, so the
     * coordinates are told by where the folder is.
     *
     * @param folder the folder of a project
     * @return the coordinates, or {@code null} for a folder that holds no project downloaded as a plain {@code zip}
     */
    public @Nullable MavenCoordinates coordinatesOf(@Nullable Path folder) {
        if (folder == null) {
            return null;
        }
        var unpacked = folder.toAbsolutePath().normalize();
        if (!unpacked.startsWith(localRepository)) {
            return null;
        }
        var path = localRepository.relativize(unpacked);
        var count = path.getNameCount();
        if (count < 4) {
            return null;
        }
        var version = path.getName(count - 2).toString();
        var artifactId = path.getName(count - 3).toString();
        // The folder of a plain archive alone: one of another extension or with a classifier is no such project
        if (!path.getFileName().toString().matches(Pattern.quote(artifactId + "-" + version + "-") + "\\d+")) {
            return null;
        }
        var groupId = path.subpath(0, count - 3).toString().replace(path.getFileSystem().getSeparator(), ".");
        return new MavenCoordinates(groupId, artifactId, version);
    }

    /**
     * Reads coordinates as the OpenL Maven Plugin does: {@code groupId:artifactId[:extension[:classifier]]:version},
     * the extension {@code zip} when it is left out.
     *
     * <p>Each part holds letters, digits and {@code _ . + -} only, and none starts with a dot, so the coordinates name
     * no file outside the repository.
     *
     * @return the artifact, or {@code null} when the coordinates are not valid
     */
    public static @Nullable Artifact artifactOf(String coordinates) {
        var parts = coordinates.trim().split(":", -1);
        if (parts.length < 3 || parts.length > 5 || !Arrays.stream(parts).allMatch(MavenDependencyResolver::isPart)) {
            return null;
        }
        var extension = parts.length >= 4 ? parts[2] : ZIP;
        var classifier = parts.length == 5 ? parts[3] : "";
        return new DefaultArtifact(parts[0], parts[1], classifier, extension, parts[parts.length - 1]);
    }

    private static boolean isPart(String part) {
        return COORDINATE_PART.matcher(part).matches();
    }

    private RepositorySystemSession.CloseableSession newSession() {
        return new SessionBuilderSupplier(system()).get()
                .withLocalRepositoryBaseDirectories(localRepository)
                // A failure is kept in memory for a while; the local repository does not keep it, so a library put
                // into the repository later is found.
                .setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(false, false))
                // A repository that does not answer fails the compilation in a minute, not in half an hour
                .setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLIS)
                .setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, REQUEST_TIMEOUT_MILLIS)
                // Every artifact is asked for, whatever the prefixes the repository publishes
                .setConfigProperty(PREFIXES_FILTER, false)
                // Only the one repository set is read, not those the POMs of the dependencies declare, Maven Central
                // of the super POM among them
                .setIgnoreArtifactDescriptorRepositories(true)
                // Every file a resolution reads is used: a library, an archive, and the POMs its dependencies are
                // worked out from, which a cleanup must not take away from under the files it keeps.
                .withRepositoryListener(new AbstractRepositoryListener() {
                    @Override
                    public void artifactResolved(RepositoryEvent event) {
                        var path = event.getPath();
                        if (path != null && event.getExceptions().isEmpty()) {
                            local.markUsed(path);
                        }
                    }
                })
                // What the repository missed a while ago is not asked for again
                .withRepositoryListener(missing)
                .build();
    }

    private RepositorySystem system() {
        var current = system.get();
        if (current == null) {
            synchronized (system) {
                current = system.get();
                if (current == null) {
                    current = new JdkRepositorySystemSupplier(missing).get();
                    system.set(current);
                }
            }
        }
        return current;
    }

    @Override
    public void destroy() {
        var current = system.get();
        if (current != null) {
            current.shutdown();
        }
    }

    private static OpenLMessage downloadError(Exception e) {
        return error("Cannot download a Maven dependency: " + e.getMessage());
    }

    private static OpenLMessage error(String summary) {
        return new OpenLMessage(summary, Severity.ERROR);
    }

    /**
     * Libraries downloaded for a project.
     *
     * @param files    the files of the libraries found, each with the libraries it needs at runtime
     * @param provided the files of the libraries the application running the project provides, which OpenL Studio
     *                 does not ship itself
     * @param messages what went wrong
     */
    public record Libraries(List<Path> files, List<Path> provided, Collection<OpenLMessage> messages) {
        static final Libraries NONE = new Libraries(List.of(), List.of(), List.of());
    }

    /**
     * An OpenL project downloaded for a dependency.
     *
     * @param project the project, or {@code null} when it cannot be downloaded or read
     * @param problem why there is no project, or {@code null} when nothing went wrong
     */
    public record DownloadedProject(@Nullable ProjectDescriptor project, @Nullable OpenLMessage problem) {
        static final DownloadedProject NONE = new DownloadedProject(null, null);
    }

    /**
     * The OpenL projects a {@code pom.xml} depends on.
     *
     * @param coordinatesByName the coordinates of the archive of each project, by the name its {@code rules.xml} gives
     * @param messages          what went wrong
     */
    public record PomProjects(Map<String, String> coordinatesByName, Collection<OpenLMessage> messages) {
        static final PomProjects NONE = new PomProjects(Map.of(), List.of());
    }

    /** What the projects of a {@code pom.xml} are kept by: its text. */
    private record PomProjectsKey(ProjectPom pom) {
    }

    /**
     * What the modules of a build are kept by: the text and the path of its topmost parent, whichever version of a
     * module asks, since each module of a design repository has the version of its own last change.
     */
    private record ReactorKey(String text, String path) {
    }

    /** What libraries are kept by: the coordinates {@code rules.xml} names and the {@code pom.xml}, if any. */
    private record LibrariesKey(List<String> coordinates, @Nullable ProjectPom pom, boolean withProvided) {
    }

    /** What a downloaded project is kept by: the name it must have, if any, and its coordinates. */
    private record ProjectKey(@Nullable String name, String coordinates) {
    }

    /** An answer, being resolved or found, and until when it is kept. */
    private record Cached<T>(CompletableFuture<T> answer, Instant validUntil) {

        /** Whether the answer is found and its period has passed: a resolution under way is kept. */
        boolean isExpired(Instant now) {
            return answer.isDone() && !validUntil.isAfter(now);
        }
    }

    /**
     * Transports the files with the HTTP client of the JDK, so no other HTTP library is needed, and does not ask for
     * what the repository missed a while ago.
     */
    private static final class JdkRepositorySystemSupplier extends RepositorySystemSupplier {
        private final MissingArtifacts missing;

        private JdkRepositorySystemSupplier(MissingArtifacts missing) {
            this.missing = missing;
        }

        @Override
        protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
            var sources = new HashMap<>(super.createRemoteRepositoryFilterSources());
            sources.put("missing", missing);
            return sources;
        }

        @Override
        protected Map<String, TransporterFactory> createTransporterFactories() {
            var factories = new HashMap<String, TransporterFactory>();
            factories.put(FileTransporterFactory.NAME, new FileTransporterFactory());
            factories.put(JdkTransporterFactory.NAME,
                    new JdkTransporterFactory(getChecksumExtractor(), getPathProcessor()));
            return factories;
        }
    }
}
