package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import lombok.Locked;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryEvent;
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
import org.openl.studio.dependencies.service.MavenCoordinates;
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
    /** Resolutions read the local repository together; a removal from it runs alone. */
    private final ReadWriteLock repositoryLock = new ReentrantReadWriteLock();
    private final LocalMavenRepository local;
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
        this.local = new LocalMavenRepository(localRepository, clock);
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
        return librariesNamed(libraryCoordinatesOf(project.getDependencies()));
    }

    /**
     * Downloads the libraries the coordinates name, with the libraries they need at runtime.
     *
     * @param coordinates the coordinates of the libraries, as the dependencies without a name write them
     * @return the libraries found, and what went wrong; nothing while no repository is set
     */
    public Libraries librariesNamed(List<String> coordinates) {
        var distinct = coordinates.stream().map(String::trim).distinct().toList();
        if (!isEnabled() || distinct.isEmpty()) {
            return Libraries.NONE;
        }
        return cached(distinct, () -> resolveLibraries(distinct));
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

    /** The size of the files kept in the local repository, in bytes. */
    public long localRepositorySize() {
        return local.size();
    }

    /**
     * Removes from the local repository the versions no project needs: those unused for longer than the period, then,
     * while the repository is larger than the limit, those used longest ago. What the answers kept in memory name
     * stays, since the projects compiled at the moment read it.
     *
     * @param unusedFor how long a version may stay unused, or {@code null} for no limit
     * @param maxBytes  how large the repository may grow, or {@code 0} for no limit
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
        return local.clear();
    }

    /** The files the answers kept in memory name: the libraries and the unpacked projects. */
    private List<Path> filesInUse() {
        var files = new ArrayList<Path>();
        for (Cached<?> cached : cache.values()) {
            switch (cached.answer().getNow(null)) {
                case Libraries libraries -> files.addAll(libraries.files());
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

    @Locked.Read("repositoryLock")
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
            var request = new ArtifactRequest(artifact, List.of(Objects.requireNonNull(repository)), null);
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
     * @return the coordinates, or {@code null} for a folder that holds no downloaded project
     */
    public @Nullable MavenCoordinates coordinatesOf(@Nullable Path folder) {
        if (folder == null) {
            return null;
        }
        var root = localRepository.toAbsolutePath().normalize();
        var unpacked = folder.toAbsolutePath().normalize();
        if (!unpacked.startsWith(root)) {
            return null;
        }
        var path = root.relativize(unpacked);
        var count = path.getNameCount();
        if (count < 4) {
            return null;
        }
        var version = path.getName(count - 2).toString();
        var artifactId = path.getName(count - 3).toString();
        if (!path.getFileName().toString().startsWith(artifactId + "-" + version + "-")) {
            return null;
        }
        var groupId = path.subpath(0, count - 3).toString().replace(path.getFileSystem().getSeparator(), ".");
        return new MavenCoordinates(groupId, artifactId, version);
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
