package org.openl.rules.repository.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.ZipUtils;

/**
 * Finds the OpenL projects and deployments of the classpath, for {@link JarLocalRepository} and for the deployment of
 * the classpath to another repository.
 *
 * <p>A jar with {@code rules.xml}, {@code deployment.xml} or {@code deployment.yaml} at its root is a project or a
 * deployment, and so is a zip archive in the {@code /openl/} folder of the classpath.
 *
 * <p>An archive nested into a Spring Boot 3.2+ executable jar is read in place, through the file system Spring Boot
 * provides for its nested jars. Any other archive nested into another archive is skipped with a warning in the log:
 * an archive of the classic loader of Spring Boot, a zip archive inside a jar, or an archive compressed inside the
 * executable jar, which Spring Boot cannot read in place.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class ClasspathArchives {

    private static final List<String> DESCRIPTOR_FILES = List.of("rules.xml", "deployment.xml", "deployment.yaml");
    private static final String ARCHIVE_SEPARATOR = "!/";
    private static final String NESTED_SCHEME = "nested:";
    private static final String NESTED_SEPARATOR = "/!";

    private final ResourcePatternResolver resourceResolver;

    /**
     * Finds the archives of the classpath of the thread context class loader.
     */
    public ClasspathArchives() {
        this(new PathMatchingResourcePatternResolver());
    }

    /**
     * @param resourceResolver the resolver of the classpath to search
     */
    public ClasspathArchives(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Finds the projects and the deployments of the classpath, each once, in the order they are found.
     *
     * <p>A resource that cannot be read, such as one of a class loader handing out URIs of an unknown scheme, is
     * skipped with a warning in the log.
     *
     * @return the paths of the archives
     */
    public List<Path> archives() {
        var archives = new LinkedHashSet<Path>();
        try {
            scan(true, archives::add);
        } catch (IOException e) {
            log.warn("Failed to search the classpath for OpenL projects.", e);
        }
        return List.copyOf(archives);
    }

    /**
     * Finds the projects and the deployments of the classpath, for a repository to hold them.
     *
     * @return the archives by their base names
     * @throws IllegalStateException when the classpath cannot be read, or two archives have base names the repository
     *                               takes as one, such as names differing only in letter case
     */
    Map<String, Path> find() {
        var storage = new HashMap<String, Path>();
        // The archives by the keys the repository looks names up by, so names it would take as one conflict here
        var byKey = new HashMap<String, Path>();
        try {
            scan(false, path -> {
                var name = FileUtils.getBaseName(path.getFileName().toString());
                var existed = byKey.putIfAbsent(AbstractArchiveRepository.keyOf(name), path);
                if (existed == null) {
                    storage.put(name, path);
                } else if (!existed.equals(path)) {
                    throw new IllegalStateException("The resources '%s' and '%s' conflict for the same '%s' name."
                            .formatted(existed, path, name));
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize a repository.", e);
        }
        return storage;
    }

    /**
     * Passes the path of every project and deployment of the classpath to the collector.
     *
     * @param lenient whether a resource or a location that cannot be read is skipped rather than failing the search
     */
    private void scan(boolean lenient, Consumer<Path> collector) throws IOException {
        for (var descriptor : DESCRIPTOR_FILES) {
            var locationPattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + descriptor;
            for (var resource : getResources(locationPattern, lenient)) {
                collect(resource, true, lenient, collector);
            }
        }
        for (var resource : getResources("/openl/*.zip", lenient)) {
            collect(resource, false, lenient, collector);
        }
    }

    private void collect(Resource resource,
                         boolean descriptor,
                         boolean lenient,
                         Consumer<Path> collector) throws IOException {
        try {
            var path = toPath(resource.getURI(), descriptor);
            if (path == null) {
                log.warn("The resource '{}' is skipped: it is not in an archive that can be read in place.", resource);
            } else {
                collector.accept(path);
            }
        } catch (IOException | RuntimeException e) {
            if (!lenient) {
                throw e;
            }
            log.warn("The resource '{}' is skipped: it cannot be read.", resource, e);
        }
    }

    private Resource[] getResources(String locationPattern, boolean lenient) throws IOException {
        try {
            return resourceResolver.getResources(locationPattern);
        } catch (FileNotFoundException ignored) {
            return new Resource[0]; // the classpath has no such folder
        } catch (IOException | RuntimeException e) {
            if (!lenient) {
                throw e;
            }
            log.warn("Failed to search the classpath for '{}'.", locationPattern, e);
            return new Resource[0];
        }
    }

    /**
     * Finds the file of a project or of a deployment found on the classpath.
     *
     * <p>A descriptor stands for the archive holding it, and an archive stands for itself. An archive nested into a
     * Spring Boot 3.2+ executable jar stands for the path Spring Boot gives to it, which is read in place.
     *
     * @param uri        the URI of the resource
     * @param descriptor whether the resource is a descriptor of a project or of a deployment
     * @return {@code null} when a descriptor is not at the root of an archive, such as one in a folder of the
     *         classpath or in {@code BOOT-INF/classes} of Spring Boot, or when an archive is nested in a way Spring
     *         Boot cannot read in place
     */
    @Nullable Path toPath(URI uri, boolean descriptor) throws IOException {
        var location = ArchiveLocation.of(uri);
        if (descriptor) {
            if (location.entries().isEmpty()) {
                return null; // a descriptor in a folder of the classpath, which is no archive of its own
            }
            location = location.parent();
        }
        if (location.entries().isEmpty()) {
            return location.file();
        }
        if (location.entries().getLast().endsWith("/")) {
            return null; // a folder of an archive, such as BOOT-INF/classes/ of Spring Boot
        }
        return nestedPath(location);
    }

    /**
     * The path Spring Boot 3.2+ gives to an archive nested into its executable jar.
     *
     * @return {@code null} when the archive is not nested by Spring Boot 3.2+, is nested twice, or cannot be read in
     *         place, such as a compressed one or when the file system of Spring Boot for nested jars is not installed
     */
    private static @Nullable Path nestedPath(ArchiveLocation location) throws IOException {
        var entry = location.nestedEntry();
        if (entry == null) {
            return null;
        }
        try {
            var path = Path.of(new URI("nested", location.file().toUri().getPath() + NESTED_SEPARATOR + entry, null));
            // Spring Boot opens an entry when it is read, so an absent or a compressed one fails only then
            return ZipUtils.isArchive(path) ? path : null;
        } catch (URISyntaxException e) {
            throw new IOException("The archive '%s' of '%s' cannot be read.".formatted(entry, location.file()), e);
        } catch (FileSystemNotFoundException e) {
            return null; // the file system of Spring Boot for nested jars is not installed
        }
    }

    /**
     * A file of the file system and the names of the archive entries leading to a resource inside it.
     *
     * @param nested whether the location is a nested URI of Spring Boot 3.2+
     */
    private record ArchiveLocation(Path file, List<String> entries, boolean nested) {

        static ArchiveLocation of(URI uri) {
            if ("jar".equals(uri.getScheme())) {
                return ofJar(uri.getRawSchemeSpecificPart());
            } else if ("file".equals(uri.getScheme())) {
                return new ArchiveLocation(Path.of(uri), List.of(), false);
            }
            throw new IllegalArgumentException("Invalid URI scheme.");
        }

        /**
         * Parses {@code file:/app.jar!/BOOT-INF/lib/rules.jar!/rules.xml} as well as
         * {@code nested:/app.jar/!BOOT-INF/lib/rules.jar!/rules.xml}.
         */
        private static ArchiveLocation ofJar(String rawPath) {
            var parts = new ArrayDeque<>(Arrays.asList(rawPath.split(ARCHIVE_SEPARATOR)));
            var file = parts.removeFirst();
            var nested = file.startsWith(NESTED_SCHEME);
            if (nested) {
                var sep = file.lastIndexOf(NESTED_SEPARATOR);
                if (sep > -1) {
                    parts.addFirst(file.substring(sep + NESTED_SEPARATOR.length()));
                    file = file.substring(0, sep);
                }
                file = "file:" + file.substring(NESTED_SCHEME.length());
            }
            var entries = parts.stream().map(entry -> StringUtils.uriDecode(entry, StandardCharsets.UTF_8)).toList();
            return new ArchiveLocation(toFile(file), entries, nested);
        }

        /**
         * The file a {@code file:} URI names.
         *
         * <p>A URI some class loaders encode twice, such as {@code file:/my%2520app/rules.jar} for
         * {@code /my app/rules.jar}, is decoded once more, but only when the file the URI names as it is does not
         * exist and the one decoded once more does. So a folder whose name holds {@code %20} as it is stays as it is.
         */
        private static Path toFile(String rawUri) {
            try {
                var uri = new URI(rawUri);
                var file = Path.of(uri);
                if (Files.exists(file) || !uri.getSchemeSpecificPart().contains("%")) {
                    return file;
                }
                var decodedTwice = decodeOnceMore(uri);
                return decodedTwice != null && Files.exists(decodedTwice) ? decodedTwice : file;
            } catch (URISyntaxException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

        /**
         * @return {@code null} when the decoded URI is not a valid one, such as for a {@code %} not followed by two
         *         hexadecimal digits
         */
        private static @Nullable Path decodeOnceMore(URI uri) {
            try {
                return Path.of(new URI(uri.getScheme() + ":" + uri.getSchemeSpecificPart()));
            } catch (URISyntaxException ignored) {
                return null;
            }
        }

        ArchiveLocation parent() {
            return new ArchiveLocation(file, entries.subList(0, entries.size() - 1), nested);
        }

        /**
         * The name of the entry of the executable jar of Spring Boot 3.2+ the location is, such as
         * {@code BOOT-INF/lib/rules.jar}, or {@code BOOT-INF/classes/openl/rules.zip} for an archive in a folder.
         *
         * @return {@code null} when the location is not nested by Spring Boot 3.2+ or goes through two archives
         */
        @Nullable String nestedEntry() {
            var folders = entries.subList(0, entries.size() - 1);
            return nested && folders.stream().allMatch(entry -> entry.endsWith("/")) ? String.join("", entries) : null;
        }
    }
}
