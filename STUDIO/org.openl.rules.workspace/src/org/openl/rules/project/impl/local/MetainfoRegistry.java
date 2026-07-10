package org.openl.rules.project.impl.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.impl.local.ProjectMetainfo.FileBaseline;
import org.openl.util.FileUtils;
import org.openl.util.PropertiesUtils;

/**
 * Per-user registry of workspace project metainfo.
 *
 * <p>The registry keeps one properties file per project under the {@code .metainfo} folder of the user
 * workspace directory. Project folders contain only project files. A record is written when the project
 * is opened or saved, and removed when the project is closed; editing project files never changes it.
 *
 * <p>The registry is authoritative. On load it reconciles the disk state: a record without a project
 * folder is dropped, a project folder without a record is deleted, an unreadable record is dropped
 * together with its folder.
 *
 * <p>All records are cached in memory and written through. The local-changes state is not stored:
 * it is reconstructed on load by comparing project files against the recorded baselines and then
 * maintained by {@link #markDirty} notifications from the local repository.
 *
 * <p>One instance per user must be shared by all sessions and channels of that user.
 *
 * @author Yury Molchan
 */
@Slf4j
@NullMarked
public class MetainfoRegistry {

    public static final String METAINFO_FOLDER = ".metainfo";

    private static final String RECORD_SUFFIX = ".properties";
    private static final String TMP_SUFFIX = ".tmp";

    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String FORMAT_VERSION = "1";
    private static final String REPOSITORY_ID_KEY = "repository-id";
    private static final String PATH_IN_REPOSITORY_KEY = "path-in-repository";
    private static final String BRANCH_KEY = "branch";
    private static final String VERSION_KEY = "version";
    private static final String AUTHOR_KEY = "author";
    private static final String MODIFIED_AT_KEY = "modified-at-long";
    private static final String SIZE_KEY = "size";
    private static final String COMMENT_KEY = "comment";
    private static final String FILE_UNIQUE_ID_PREFIX = "file.unique-id.";
    private static final String FILE_SIZE_PREFIX = "file.size.";
    private static final String FILE_MODIFIED_AT_PREFIX = "file.modified-at-long.";

    private final Path userDir;
    private final Path metainfoDir;
    private final Map<String, ProjectMetainfo> records = new ConcurrentHashMap<>();
    private final Set<String> dirtyProjects = ConcurrentHashMap.newKeySet();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private MetainfoRegistry(Path userDir) {
        this.userDir = userDir;
        this.metainfoDir = userDir.resolve(METAINFO_FOLDER);
    }

    /**
     * Loads the registry of the given user workspace directory.
     *
     * <p>Reads all records, reconciles them with the project folders on disk, and reconstructs the
     * local-changes state from the recorded baselines.
     */
    public static MetainfoRegistry open(Path userDir) {
        var registry = new MetainfoRegistry(userDir);
        registry.load();
        return registry;
    }

    /**
     * Returns the metainfo of the project, or {@code null} when the project is not registered.
     */
    @Nullable
    public ProjectMetainfo get(String projectName) {
        return records.get(projectName);
    }

    /**
     * Returns the names of all registered projects.
     */
    public Collection<String> projects() {
        return List.copyOf(records.keySet());
    }

    /**
     * Stores the project metainfo snapshot and resets the local-changes state.
     *
     * <p>The record is written atomically, so a crash cannot leave a partially written record.
     */
    public void save(String projectName, ProjectMetainfo metainfo) {
        var lock = lockOf(projectName);
        lock.lock();
        try {
            Files.createDirectories(metainfoDir);
            var tmp = metainfoDir.resolve(projectName + RECORD_SUFFIX + TMP_SUFFIX);
            PropertiesUtils.store(tmp, toProperties(metainfo).entrySet());
            Files.move(tmp, recordFile(projectName), StandardCopyOption.ATOMIC_MOVE);
            records.put(projectName, metainfo);
            dirtyProjects.remove(projectName);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save the metainfo of the '" + projectName + "' project.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the project record and its local-changes state.
     */
    public void remove(String projectName) {
        var lock = lockOf(projectName);
        lock.lock();
        try {
            Files.deleteIfExists(recordFile(projectName));
            records.remove(projectName);
            dirtyProjects.remove(projectName);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot remove the metainfo of the '" + projectName + "' project.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Marks the project as locally changed.
     */
    public void markDirty(String projectName) {
        dirtyProjects.add(projectName);
    }

    /**
     * Returns whether the project has local changes.
     */
    public boolean isDirty(String projectName) {
        return dirtyProjects.contains(projectName);
    }

    /**
     * Returns the repository revision id of the file when its local copy is unchanged.
     *
     * <p>Returns {@code null} when the file has no recorded baseline or differs from it by size or
     * modification time, which means the local content no longer matches any known repository revision.
     */
    @Nullable
    public String uniqueId(String projectName, String path, long size, long modifiedAt) {
        var metainfo = records.get(projectName);
        if (metainfo == null) {
            return null;
        }
        var baseline = metainfo.files().get(path);
        if (baseline == null || baseline.size() != size || baseline.modifiedAt() != modifiedAt) {
            return null;
        }
        return baseline.uniqueId();
    }

    /**
     * Returns the recorded baseline of the file, or {@code null} when there is none.
     */
    @Nullable
    public FileBaseline baseline(String projectName, String path) {
        var metainfo = records.get(projectName);
        return metainfo == null ? null : metainfo.files().get(path);
    }

    private ReentrantLock lockOf(String projectName) {
        return locks.computeIfAbsent(projectName, name -> new ReentrantLock());
    }

    private Path recordFile(String projectName) {
        return metainfoDir.resolve(projectName + RECORD_SUFFIX);
    }

    // --- loading and reconciliation

    private void load() {
        var corrupted = new HashSet<String>();
        readRecords(corrupted);
        var folders = listProjectFolders();
        dropCorrupted(corrupted, folders);
        dropRecordsWithoutFolder(folders);
        dropFoldersWithoutRecord(folders);
        reconstructDirtyState();
    }

    private void readRecords(Set<String> corrupted) {
        if (!Files.isDirectory(metainfoDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(metainfoDir)) {
            stream.filter(Files::isRegularFile).forEach(file -> readRecord(file, corrupted));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list the metainfo registry at " + metainfoDir, e);
        }
    }

    private void readRecord(Path file, Set<String> corrupted) {
        var fileName = file.getFileName().toString();
        if (fileName.endsWith(TMP_SUFFIX)) {
            // A leftover of an interrupted write. The target record is intact, the leftover is garbage.
            FileUtils.deleteQuietly(file.toFile());
            return;
        }
        if (!fileName.endsWith(RECORD_SUFFIX)) {
            return;
        }
        var projectName = fileName.substring(0, fileName.length() - RECORD_SUFFIX.length());
        try {
            records.put(projectName, parse(file));
        } catch (IOException | RuntimeException e) {
            log.error("The metainfo record of the '{}' project is unreadable and will be dropped together"
                    + " with the project folder.", projectName, e);
            corrupted.add(projectName);
        }
    }

    private Set<String> listProjectFolders() {
        var folders = new HashSet<String>();
        if (!Files.isDirectory(userDir)) {
            return folders;
        }
        try (Stream<Path> stream = Files.list(userDir)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.startsWith("."))
                    .forEach(folders::add);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list the user workspace at " + userDir, e);
        }
        return folders;
    }

    private void dropCorrupted(Set<String> corrupted, Set<String> folders) {
        for (String projectName : corrupted) {
            FileUtils.deleteQuietly(recordFile(projectName).toFile());
            if (folders.remove(projectName)) {
                FileUtils.deleteQuietly(userDir.resolve(projectName).toFile());
            }
        }
    }

    private void dropRecordsWithoutFolder(Set<String> folders) {
        for (String projectName : List.copyOf(records.keySet())) {
            if (!folders.contains(projectName)) {
                log.info("The metainfo record of the '{}' project has no project folder and is dropped.",
                        projectName);
                remove(projectName);
            }
        }
    }

    private void dropFoldersWithoutRecord(Set<String> folders) {
        for (String folder : folders) {
            if (!records.containsKey(folder)) {
                log.warn("The '{}' folder in the user workspace has no metainfo record and is deleted.", folder);
                FileUtils.deleteQuietly(userDir.resolve(folder).toFile());
            }
        }
    }

    private void reconstructDirtyState() {
        records.forEach((projectName, metainfo) -> {
            if (hasLocalChanges(projectName, metainfo)) {
                dirtyProjects.add(projectName);
            }
        });
    }

    private boolean hasLocalChanges(String projectName, ProjectMetainfo metainfo) {
        var projectDir = userDir.resolve(projectName);
        var unseen = new HashSet<>(metainfo.files().keySet());
        try (Stream<Path> stream = Files.walk(projectDir)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                if (differsFromBaseline(projectDir, file, metainfo, unseen)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.warn("Cannot inspect the local changes of the '{}' project. The project is considered changed.",
                    projectName, e);
            return true;
        }
        // Baselines left unseen belong to files deleted locally.
        return !unseen.isEmpty();
    }

    private boolean differsFromBaseline(Path projectDir,
                                        Path file,
                                        ProjectMetainfo metainfo,
                                        Set<String> unseen) throws IOException {
        var path = "/" + projectDir.relativize(file).toString().replace('\\', '/');
        var baseline = metainfo.files().get(path);
        if (baseline == null) {
            return true;
        }
        unseen.remove(path);
        return baseline.size() != Files.size(file)
                || baseline.modifiedAt() != Files.getLastModifiedTime(file).toMillis();
    }

    // --- record format

    private static ProjectMetainfo parse(Path file) throws IOException {
        var properties = new LinkedHashMap<String, String>();
        PropertiesUtils.load(file, properties::put);
        if (!FORMAT_VERSION.equals(properties.get(FORMAT_VERSION_KEY))) {
            throw new IllegalStateException("Unsupported metainfo format: " + properties.get(FORMAT_VERSION_KEY));
        }
        var repositoryId = properties.get(REPOSITORY_ID_KEY);
        if (repositoryId == null) {
            throw new IllegalStateException("The repository id is absent.");
        }
        return new ProjectMetainfo(repositoryId,
                properties.get(PATH_IN_REPOSITORY_KEY),
                properties.get(BRANCH_KEY),
                properties.get(VERSION_KEY),
                properties.get(AUTHOR_KEY),
                parseLong(properties.get(MODIFIED_AT_KEY)),
                parseLong(properties.get(SIZE_KEY)),
                properties.get(COMMENT_KEY),
                parseBaselines(properties));
    }

    private static Map<String, FileBaseline> parseBaselines(Map<String, String> properties) {
        var uniqueIds = new HashMap<String, String>();
        var sizes = new HashMap<String, String>();
        var modifiedAts = new HashMap<String, String>();
        properties.forEach((key, value) -> {
            if (key.startsWith(FILE_UNIQUE_ID_PREFIX)) {
                uniqueIds.put(key.substring(FILE_UNIQUE_ID_PREFIX.length()), value);
            } else if (key.startsWith(FILE_SIZE_PREFIX)) {
                sizes.put(key.substring(FILE_SIZE_PREFIX.length()), value);
            } else if (key.startsWith(FILE_MODIFIED_AT_PREFIX)) {
                modifiedAts.put(key.substring(FILE_MODIFIED_AT_PREFIX.length()), value);
            }
        });
        var baselines = new HashMap<String, FileBaseline>();
        for (var entry : sizes.entrySet()) {
            var path = entry.getKey();
            var modifiedAt = modifiedAts.get(path);
            if (modifiedAt == null) {
                throw new IllegalStateException("The baseline of '" + path + "' has no modification time.");
            }
            baselines.put(path,
                    new FileBaseline(uniqueIds.get(path), Long.parseLong(entry.getValue()),
                            Long.parseLong(modifiedAt)));
        }
        return baselines;
    }

    @Nullable
    private static Long parseLong(@Nullable String value) {
        return value == null ? null : Long.valueOf(value);
    }

    private static Map<String, String> toProperties(ProjectMetainfo metainfo) {
        var properties = new LinkedHashMap<String, String>();
        properties.put(FORMAT_VERSION_KEY, FORMAT_VERSION);
        properties.put(REPOSITORY_ID_KEY, metainfo.repositoryId());
        putIfPresent(properties, PATH_IN_REPOSITORY_KEY, metainfo.pathInRepository());
        putIfPresent(properties, BRANCH_KEY, metainfo.branch());
        putIfPresent(properties, VERSION_KEY, metainfo.version());
        putIfPresent(properties, AUTHOR_KEY, metainfo.author());
        putIfPresent(properties, MODIFIED_AT_KEY, metainfo.modifiedAt() == null ? null
                : metainfo.modifiedAt().toString());
        putIfPresent(properties, SIZE_KEY, metainfo.size() == null ? null : metainfo.size().toString());
        putIfPresent(properties, COMMENT_KEY, metainfo.comment());
        metainfo.files()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    var path = entry.getKey();
                    var baseline = entry.getValue();
                    putIfPresent(properties, FILE_UNIQUE_ID_PREFIX + path, baseline.uniqueId());
                    properties.put(FILE_SIZE_PREFIX + path, Long.toString(baseline.size()));
                    properties.put(FILE_MODIFIED_AT_PREFIX + path, Long.toString(baseline.modifiedAt()));
                });
        return properties;
    }

    private static void putIfPresent(Map<String, String> properties, String key, @Nullable String value) {
        if (value != null) {
            properties.put(key, value);
        }
    }
}
