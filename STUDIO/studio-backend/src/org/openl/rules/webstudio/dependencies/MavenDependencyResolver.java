package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
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
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

/**
 * Downloads from a Maven repository the dependencies a {@code rules.xml} names by Maven coordinates.
 *
 * <p>A dependency with no {@code <name>} is a library: it is downloaded with the libraries it needs at runtime, and
 * OpenL Studio puts them on the classpath of the project. A dependency with a {@code <name>} is an OpenL project:
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

    private static final String ZIP = "zip";
    private static final String REPOSITORY_ID = "openl";
    /** A part of coordinates: no path separator, so it names no file outside the local repository. */
    private static final Pattern COORDINATE_PART = Pattern.compile("[\\w.+-]+");
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int REQUEST_TIMEOUT_MILLIS = 60_000;

    private final @Nullable RemoteRepository repository;
    private final Path localRepository;
    private final Clock clock;
    private final Map<Object, Cached<?>> cache = new ConcurrentHashMap<>();
    private final Object extractLock = new Object();
    private volatile @Nullable RepositorySystem system;

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
        this.localRepository = localRepository;
        this.clock = clock;
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
     * Downloads the libraries the project names by Maven coordinates, with the libraries they need at runtime.
     *
     * @param project the project
     * @return the libraries found, and what went wrong; nothing while no repository is set
     */
    public Libraries librariesOf(ProjectDescriptor project) {
        var coordinates = libraryCoordinatesOf(project.getDependencies());
        if (!isEnabled() || coordinates.isEmpty()) {
            return Libraries.NONE;
        }
        return cached(coordinates, () -> resolveLibraries(coordinates));
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
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * The dependencies that name an OpenL project by Maven coordinates: those with both a name and coordinates.
     *
     * @param dependencies the dependencies a {@code rules.xml} declares, or {@code null} when it declares none
     * @return the dependencies, in the order they are declared
     */
    public static List<ProjectDependencyDescriptor> namedMavenDependenciesOf(
            @Nullable Collection<ProjectDependencyDescriptor> dependencies) {
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .filter(dependency -> dependency.getName() != null && dependency.getMavenArtifact() != null)
                .toList();
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
            } catch (RuntimeException e) {
                cache.remove(key, fresh);
                fresh.answer().completeExceptionally(e);
                throw e;
            }
        }
        return current.answer().join();
    }

    /** How many answers are kept in memory. */
    int cachedAnswers() {
        return cache.size();
    }

    private Libraries resolveLibraries(List<String> coordinates) {
        var messages = new ArrayList<OpenLMessage>();
        var collect = new CollectRequest();
        collect.setRepositories(List.of(Objects.requireNonNull(repository)));
        for (String coordinate : coordinates) {
            var artifact = artifactOf(coordinate);
            if (artifact == null) {
                messages.add(error("The Maven coordinates '%s' are not valid.".formatted(coordinate)));
            } else {
                collect.addDependency(new Dependency(artifact, JavaScopes.RUNTIME));
            }
        }
        if (collect.getDependencies().isEmpty()) {
            return new Libraries(List.of(), messages);
        }
        var request = new DependencyRequest(collect, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));
        List<ArtifactResult> results;
        try (var session = newSession()) {
            results = resolve(session, request, messages);
        }
        var files = new ArrayList<Path>();
        for (ArtifactResult result : results) {
            if (result.isResolved()) {
                files.add(result.getArtifact().getPath());
            } else {
                result.getExceptions().forEach(e -> messages.add(downloadError(e)));
            }
        }
        return new Libraries(files, messages);
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

    private DownloadedProject resolveProject(String name, String coordinates) {
        var artifact = artifactOf(coordinates);
        if (artifact == null) {
            return new DownloadedProject(null,
                    error("The Maven coordinates '%s' of the dependency '%s' are not valid.".formatted(coordinates,
                            name)));
        }
        Path archive;
        try (var session = newSession()) {
            var request = new ArtifactRequest(artifact, List.of(Objects.requireNonNull(repository)), null);
            archive = system().resolveArtifact(session, request).getArtifact().getPath();
        } catch (ArtifactResolutionException e) {
            log.warn("Cannot download the project '{}' from '{}'.", name, coordinates, e);
            return new DownloadedProject(null, downloadError(e));
        }
        try {
            var project = ProjectResolver.getInstance().resolve(unpacked(archive));
            if (project == null) {
                return new DownloadedProject(null,
                        error("'%s' of the dependency '%s' is not an OpenL project.".formatted(coordinates, name)));
            }
            if (!name.equals(project.getName())) {
                return new DownloadedProject(null,
                        error("'%s' is the project '%s', not the dependency '%s'.".formatted(coordinates,
                                project.getName(),
                                name)));
            }
            return new DownloadedProject(project, null);
        } catch (IOException | ProjectResolvingException e) {
            log.warn("Cannot read the project '{}' downloaded from '{}'.", name, coordinates, e);
            return new DownloadedProject(null,
                    error("Cannot read the project '%s' downloaded from '%s': %s".formatted(name,
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
     * Reads coordinates as the OpenL Maven Plugin does: {@code groupId:artifactId[:extension[:classifier]]:version},
     * the extension {@code zip} when it is left out.
     *
     * <p>Each part holds letters, digits and {@code _ . + -} only, and none is {@code .} or {@code ..}, so the
     * coordinates name no file outside the repository.
     *
     * @return the artifact, or {@code null} when the coordinates are not valid
     */
    static @Nullable Artifact artifactOf(String coordinates) {
        var parts = coordinates.trim().split(":", -1);
        if (parts.length < 3 || parts.length > 5 || !Arrays.stream(parts).allMatch(MavenDependencyResolver::isPart)) {
            return null;
        }
        var extension = parts.length >= 4 ? parts[2] : ZIP;
        var classifier = parts.length == 5 ? parts[3] : "";
        return new DefaultArtifact(parts[0], parts[1], classifier, extension, parts[parts.length - 1]);
    }

    private static boolean isPart(String part) {
        return COORDINATE_PART.matcher(part).matches() && !part.equals(".") && !part.equals("..");
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
                .build();
    }

    private RepositorySystem system() {
        var current = system;
        if (current == null) {
            synchronized (this) {
                current = system;
                if (current == null) {
                    current = new JdkRepositorySystemSupplier().get();
                    system = current;
                }
            }
        }
        return current;
    }

    @Override
    public void destroy() {
        var current = system;
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
     * @param messages what went wrong
     */
    public record Libraries(List<Path> files, Collection<OpenLMessage> messages) {
        static final Libraries NONE = new Libraries(List.of(), List.of());
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

    private record ProjectKey(String name, String coordinates) {
    }

    /** An answer, being resolved or found, and until when it is kept. */
    private record Cached<T>(CompletableFuture<T> answer, Instant validUntil) {

        /** Whether the answer is found and its period has passed: a resolution under way is kept. */
        boolean isExpired(Instant now) {
            return answer.isDone() && !validUntil.isAfter(now);
        }
    }

    /** Transports the files with the HTTP client of the JDK, so no other HTTP library is needed. */
    private static final class JdkRepositorySystemSupplier extends RepositorySystemSupplier {
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
