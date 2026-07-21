package org.openl.rules.project.impl.local;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
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
import org.openl.rules.workspace.lw.impl.FolderHelper;
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
 * together with its folder, and a leftover of an interrupted record write is deleted. The same
 * reconciliation can be re-run on a live registry with {@link #refresh()}, for example on user sign-in.
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
        registry.refresh();
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
            store(userDir, projectName, metainfo);
            records.put(projectName, metainfo);
            dirtyProjects.remove(projectName);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save the metainfo of the '" + projectName + "' project.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns whether the project of the given user workspace has a record on disk.
     */
    public static boolean exists(Path userDir, String projectName) {
        return Files.isRegularFile(userDir.resolve(METAINFO_FOLDER).resolve(projectName + RECORD_SUFFIX));
    }

    /**
     * Writes one project record of the given user workspace directly to disk.
     *
     * <p>Serves the one-time migration of legacy workspaces, which runs before any registry is loaded.
     * The record is written atomically.
     */
    public static void store(Path userDir, String projectName, ProjectMetainfo metainfo) throws IOException {
        var metainfoDir = userDir.resolve(METAINFO_FOLDER);
        Files.createDirectories(metainfoDir);
        var tmp = metainfoDir.resolve(projectName + RECORD_SUFFIX + TMP_SUFFIX);
        PropertiesUtils.store(tmp, toProperties(metainfo).entrySet());
        var target = metainfoDir.resolve(projectName + RECORD_SUFFIX);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
            // ATOMIC_MOVE ignores REPLACE_EXISTING, and overwriting an existing target is
            // implementation-specific, so retry as a plain replacing move.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
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
     * Renames the project record following a project folder rename.
     *
     * <p>The local-changes state moves together with the record. Fails when the project is not
     * registered or the new name is already registered, so the caller can undo the folder rename
     * instead of leaving the folder and the record under different names.
     */
    public void rename(String projectName, String newProjectName) {
        if (projectName.equals(newProjectName)) {
            return;
        }
        if (!FolderHelper.isSafeFolderName(newProjectName)) {
            throw new IllegalStateException("The '" + newProjectName + "' name is not a valid folder name.");
        }
        // Both names are locked, so a concurrent save or rename of either project cannot desync
        // the cache from the records on disk.
        runLocked(projectName, newProjectName, () -> {
            var metainfo = records.get(projectName);
            if (metainfo == null) {
                throw new IllegalStateException("The '" + projectName + "' project is not registered.");
            }
            if (records.containsKey(newProjectName) || Files.exists(recordFile(newProjectName))) {
                throw new IllegalStateException("The '" + newProjectName + "' project is already registered.");
            }
            try {
                Files.move(recordFile(projectName), recordFile(newProjectName), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot rename the metainfo of the '" + projectName + "' project to '" + newProjectName + "'.",
                        e);
            }
            records.remove(projectName);
            records.put(newProjectName, metainfo);
            if (dirtyProjects.remove(projectName)) {
                dirtyProjects.add(newProjectName);
            }
            return null;
        });
    }

    /**
     * Renames the project folder together with its record.
     *
     * <p>Serves the workspace synchronization when the project was renamed on the design side. A name
     * unusable as a folder name is rejected: it would escape the workspace or become a hidden service
     * folder. When the record rename fails, the folder rename is rolled back — a folder without a
     * record is garbage for the reconciliation, so the pair must stay consistent.
     *
     * @return whether the project was renamed
     */
    public boolean renameProjectFolder(String projectName, String newProjectName) {
        if (!FolderHelper.isSafeFolderName(newProjectName)) {
            log.warn("The new name '{}' of the '{}' project is not a valid folder name."
                    + " The rename is skipped.", newProjectName, projectName);
            return false;
        }
        return Boolean.TRUE.equals(runLocked(projectName, newProjectName, () -> {
            try {
                Files.move(userDir.resolve(projectName), userDir.resolve(newProjectName));
            } catch (IOException e) {
                log.warn("Cannot rename the project folder from {} to {}", projectName, newProjectName, e);
                return false;
            }
            try {
                rename(projectName, newProjectName);
                return true;
            } catch (RuntimeException e) {
                log.error("Cannot rename the metainfo of the '{}' project to '{}'."
                        + " The folder rename is rolled back.", projectName, newProjectName, e);
                rollbackFolderRename(projectName, newProjectName);
                return false;
            }
        }));
    }

    private void rollbackFolderRename(String projectName, String newProjectName) {
        try {
            Files.move(userDir.resolve(newProjectName), userDir.resolve(projectName));
        } catch (IOException e) {
            log.error("Cannot roll back the folder rename from {} to {}", newProjectName, projectName, e);
        }
    }

    /**
     * Relinks the registered project to another repository revision.
     *
     * <p>The recorded file baselines are preserved, and the local-changes state is not reset. The whole
     * replacement runs under the project lock, so a concurrent editing notification cannot be lost.
     */
    public void relink(String projectName, ProjectMetainfo metainfo) {
        var lock = lockOf(projectName);
        lock.lock();
        try {
            var previous = records.get(projectName);
            var merged = previous == null ? metainfo : metainfo.withFiles(previous.files());
            store(userDir, projectName, merged);
            records.put(projectName, merged);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot relink the metainfo of the '" + projectName + "' project.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the action under the project lock.
     *
     * <p>A multi-step operation whose intermediate state must not be observed by a concurrent
     * {@link #refresh()} runs under this lock. Opening a project copies the project files and then
     * writes the record; without the lock the refresh could treat the half-copied folder as garbage.
     *
     * <p>The lock is reentrant: registry operations on the same project may be called inside the
     * action.
     */
    @Nullable
    public <T, E extends Exception> T runLocked(String projectName, LockedAction<T, E> action) throws E {
        return runLocked(projectName, projectName, action);
    }

    /**
     * Runs the action under the locks of two projects, for example a rename of the project folder
     * together with its record.
     *
     * <p>Both locks are taken in a deterministic order, so two concurrent callers cannot deadlock.
     * The locks are reentrant, so both names may be the same project.
     */
    @Nullable
    private <T, E extends Exception> T runLocked(String projectName,
                                                 String secondProjectName,
                                                 LockedAction<T, E> action) throws E {
        boolean directOrder = projectName.compareTo(secondProjectName) <= 0;
        var first = lockOf(directOrder ? projectName : secondProjectName);
        var second = lockOf(directOrder ? secondProjectName : projectName);
        first.lock();
        second.lock();
        try {
            return action.run();
        } finally {
            second.unlock();
            first.unlock();
        }
    }

    /**
     * An action executed under the project lock by {@link #runLocked}.
     */
    @FunctionalInterface
    public interface LockedAction<T, E extends Exception> {
        @Nullable
        T run() throws E;
    }

    /**
     * Marks the project as locally changed.
     *
     * <p>The mark is taken under the project lock, so a concurrent {@link #refresh()} cannot lose the
     * notification. No IO is performed.
     */
    public void markDirty(String projectName) {
        var lock = lockOf(projectName);
        lock.lock();
        try {
            dirtyProjects.add(projectName);
        } finally {
            lock.unlock();
        }
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

    // --- reconciliation

    /**
     * Runs the disk reconciliation and recomputes the local-changes state of every project.
     *
     * <p>The reconciliation outcomes are listed in the class description. The registry may be live:
     * every project is reconciled under its project lock, and a project locked by an operation in
     * progress is skipped — the operation rewrites the project state anyway, and the refresh must
     * not stall behind it.
     */
    public void refresh() {
        var names = new HashSet<String>();
        names.addAll(records.keySet());
        names.addAll(listRecordNames());
        names.addAll(listProjectFolders());
        names.forEach(this::refreshProject);
    }

    private void refreshProject(String projectName) {
        var lock = lockOf(projectName);
        if (!lock.tryLock()) {
            log.debug("The '{}' project is inside an operation and is skipped by the refresh.", projectName);
            return;
        }
        try {
            var recordFile = recordFile(projectName);
            var leftover = metainfoDir.resolve(recordFile.getFileName() + TMP_SUFFIX);
            if (Files.exists(leftover)) {
                // A leftover of an interrupted record write. Under the project lock it cannot belong
                // to a store in progress, so it is garbage. The target record is intact.
                FileUtils.deleteQuietly(leftover.toFile());
            }
            boolean hasFolder = Files.isDirectory(userDir.resolve(projectName));
            if (!Files.isRegularFile(recordFile)) {
                records.remove(projectName);
                dirtyProjects.remove(projectName);
                if (hasFolder) {
                    deleteStrayFolder(projectName);
                }
                return;
            }
            ProjectMetainfo metainfo;
            try {
                metainfo = parse(recordFile);
            } catch (IOException | RuntimeException e) {
                log.error("The metainfo record of the '{}' project is unreadable and will be dropped together"
                        + " with the project folder.", projectName, e);
                dropCorrupted(projectName);
                return;
            }
            if (!hasFolder) {
                log.info("The metainfo record of the '{}' project has no project folder and is dropped.",
                        projectName);
                remove(projectName);
                return;
            }
            records.put(projectName, metainfo);
            recomputeLocalChanges(projectName, metainfo);
        } finally {
            lock.unlock();
        }
    }

    private void recomputeLocalChanges(String projectName, ProjectMetainfo metainfo) {
        if (hasLocalChanges(projectName, metainfo)) {
            dirtyProjects.add(projectName);
        } else {
            dirtyProjects.remove(projectName);
        }
    }

    private Set<String> listRecordNames() {
        var names = new HashSet<String>();
        if (!Files.isDirectory(metainfoDir)) {
            return names;
        }
        try (Stream<Path> stream = Files.list(metainfoDir)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    // A leftover of an interrupted record write also identifies its project, so an
                    // orphaned leftover is visited and cleaned like any other project name.
                    .map(name -> name.endsWith(TMP_SUFFIX)
                            ? name.substring(0, name.length() - TMP_SUFFIX.length())
                            : name)
                    .filter(name -> name.endsWith(RECORD_SUFFIX))
                    .map(name -> name.substring(0, name.length() - RECORD_SUFFIX.length()))
                    .forEach(names::add);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list the metainfo registry at " + metainfoDir, e);
        }
        return names;
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

    private void dropCorrupted(String projectName) {
        FileUtils.deleteQuietly(recordFile(projectName).toFile());
        FileUtils.deleteQuietly(userDir.resolve(projectName).toFile());
        records.remove(projectName);
        dirtyProjects.remove(projectName);
    }

    private void deleteStrayFolder(String folder) {
        log.warn("The '{}' folder in the user workspace has no metainfo record and is deleted.", folder);
        FileUtils.deleteQuietly(userDir.resolve(folder).toFile());
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
