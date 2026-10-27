package org.openl.rules.repository.file;

import static java.nio.file.attribute.FileTime.fromMillis;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.IgnoringRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.FileLockedException;
import org.openl.util.FileReplacement;
import org.openl.util.FileUtils;

/**
 * A simple implementation of a file-system-based repository. This repository does not support versioning.
 *
 * <p>The {@link IgnoredPaths ignored paths} are out of reach: an ignored file is not listed and reading it finds
 * nothing, writing it is refused with {@link AccessDeniedException}, and a deletion that names it deletes nothing. A
 * deleted folder goes with everything in it, its ignored files included, as a deleted project does. A path a request
 * names is matched as the file system resolves it too, so neither a short name of Windows, such as {@code GIT~1}, nor
 * a link gets around the patterns. A save of a whole folder keeps the ignored files it holds, and the changes of an
 * ignored folder are not watched.
 *
 * @author Yury Molchan
 */
@Slf4j
public class FileSystemRepository implements Repository, IgnoringRepository, Closeable {

    @Getter
    @Setter
    private Path root;
    private ChangesMonitor monitor;
    @Getter
    @Setter
    private String id;
    @Setter
    private int listenerTimerPeriod = 10;
    @Getter
    @Setter
    private String name;
    /** The paths the repository neither shows nor changes. */
    @Getter
    private IgnoredPaths ignoredPaths = IgnoredPaths.of(null);
    /** The root as the file system resolves it, kept while the root stays the same. */
    private @Nullable ResolvedRoot resolvedRoot;

    public void setUri(String path) {
        this.root = Path.of(path);
    }

    @Override
    public void setIgnoredPaths(String setting) {
        ignore(IgnoredPaths.of(setting));
    }

    /**
     * Keeps the given paths out of reach. Call it before the repository is used.
     */
    public final void ignore(IgnoredPaths paths) {
        this.ignoredPaths = paths;
    }

    public void initialize() {
        // The root folder is used as it is.
    }

    /**
     * Resolves a repository path against the root folder and keeps it inside the root.
     *
     * A path that points outside the root, such as an absolute path or one with leading parent
     * segments, is rejected. This keeps listing and traversal within the repository.
     *
     * @param path repository path to resolve
     * @return the resolved path located inside the repository root
     * @throws InvalidPathException if the path points outside the repository root
     */
    private Path resolveInRoot(String path) {
        var base = root.toAbsolutePath().normalize();
        var resolved = root.resolve(path);
        if (!resolved.toAbsolutePath().normalize().startsWith(base)) {
            throw new InvalidPathException(path, "Path is outside the repository root");
        }
        return resolved;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        var directory = resolveInRoot(path);
        var list = new ArrayList<FileData>();
        if (Files.isDirectory(directory) && !isIgnored(directory)) {
            walkListed(directory, file -> {
                if (!FileReplacement.isTemporary(file)) {
                    addFileData(list, file, directory);
                }
            });
        }
        return list;
    }

    /**
     * Walks the files of a folder the listings show: a file, or a link to one, whose path is not ignored. An ignored
     * folder is not read.
     */
    void walkListed(Path directory, Consumer<Path> onFile) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return isListed(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                var regular = attrs.isRegularFile() || attrs.isSymbolicLink() && Files.isRegularFile(file);
                if (regular && isListed(file)) {
                    onFile.accept(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void addFileData(List<FileData> list, Path file, Path directory) {
        try {
            list.add(getFileData(file));
        } catch (Exception ex) {
            log.warn("Failed to resolve file '{}' in the directory '{}'.", file.getFileName(), directory, ex);
        }
    }

    @Override
    public @Nullable FileData check(String name) throws IOException {
        var file = resolveInRoot(name);
        if (Files.exists(file) && !isIgnored(file)) {
            return getFileData(file);
        }
        return null;
    }

    @Override
    public @Nullable FileItem read(String name) throws IOException {
        var file = resolveInRoot(name);
        if (Files.exists(file) && !isIgnored(file)) {
            var data = getFileData(file);
            var stream = Files.newInputStream(file);
            return new FileItem(data, stream);
        }
        return null;
    }

    /**
     * Tells whether a file or a folder in the repository is ignored, such as the output folder of a build found in a
     * project folder. A path outside the repository is not.
     */
    public boolean ignores(Path file) {
        var absolute = file.toAbsolutePath().normalize();
        var base = root.toAbsolutePath().normalize();
        return absolute.startsWith(base)
                && ignoredPaths.matches(base.relativize(absolute).toString().replace('\\', '/'));
    }

    /**
     * Tells whether a file or a folder met while listing is shown: its path is not ignored. A listing reads the names
     * the file system gives, so the path is matched as it is.
     */
    boolean isListed(Path file) {
        return !ignoredPaths.matches(nameOf(file));
    }

    /**
     * Tells whether a path a request names is ignored. It is matched as it is written and as the file system
     * resolves it, the links and the short names of Windows followed.
     */
    private boolean isIgnored(Path file) {
        return ignoredPaths.matches(nameOf(file)) || ignoredPaths.matches(realNameOf(file));
    }

    /**
     * The path of a file as the file system resolves it, relative to the root of the repository when it is inside it.
     * A file that does not exist yet is resolved by its nearest folder that exists. A path that cannot be resolved is
     * returned as it is written.
     */
    private String realNameOf(Path file) {
        var absolute = file.toAbsolutePath().normalize();
        var missing = new ArrayDeque<Path>();
        var existing = absolute;
        while (existing.getParent() != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            missing.push(existing.getFileName());
            existing = existing.getParent();
        }
        try {
            var real = existing.toRealPath();
            for (var part : missing) {
                real = real.resolve(part);
            }
            var realRoot = realRoot();
            var relative = real.startsWith(realRoot) ? realRoot.relativize(real) : real.subpath(0, real.getNameCount());
            return relative.toString().replace('\\', '/');
        } catch (IOException e) {
            return nameOf(file);
        }
    }

    private Path realRoot() throws IOException {
        var resolved = resolvedRoot;
        if (resolved == null || !resolved.root().equals(root)) {
            resolved = new ResolvedRoot(root, root.toRealPath());
            resolvedRoot = resolved;
        }
        return resolved.real();
    }

    private record ResolvedRoot(Path root, Path real) {
    }

    /** The path of a file relative to the root of the repository, its folders separated by {@code /}. */
    private String nameOf(Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private void refuseIgnored(Path file) throws AccessDeniedException {
        if (isIgnored(file)) {
            throw new AccessDeniedException(nameOf(file), null, "The path is ignored by the repository.");
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        var saved = write(data, stream);
        invokeListener();
        return saved;
    }

    private FileData write(FileData data, InputStream stream) throws IOException {
        var file = resolveInRoot(data.getName());
        refuseIgnored(file);
        return writeTo(file, data, stream);
    }

    private FileData writeTo(Path file, FileData data, InputStream stream) throws IOException {
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        replace(stream, file);

        if (data.getModifiedAt() != null) {
            Files.setLastModifiedTime(file, fromMillis(data.getModifiedAt().getTime()));
        }
        return getFileData(file);
    }

    /**
     * {@inheritDoc}
     *
     * <p>An ignored file is left out.
     */
    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var result = new ArrayList<FileData>();
        for (FileItem fileItem : fileItems) {
            var file = resolveInRoot(fileItem.getData().getName());
            if (!isIgnored(file)) {
                result.add(writeTo(file, fileItem.getData(), fileItem.getStream()));
            }
        }
        invokeListener();
        return result;
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        var file = resolveInRoot(data.getName());
        if (isIgnored(file)) {
            return false;
        }
        try {
            // A folder is deleted with everything in it, which deleting a project relies on.
            FileUtils.delete(file);
        } catch (FileNotFoundException e) {
            return false;
        } catch (FileSystemException e) {
            throw locked(e);
        }

        deleteEmptyParentFolders(file);

        invokeListener();
        return true;
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        var deleted = false;
        for (var fd : data) {
            var f = resolveInRoot(fd.getName());
            deleted |= !isIgnored(f) && deleteQuietly(f);
        }
        if (deleted) {
            invokeListener();
        }
        return deleted;
    }

    /**
     * Deletes a file and the folders it leaves empty.
     *
     * @return {@code false} when the file cannot be deleted, for example, when it does not exist
     */
    private boolean deleteQuietly(Path file) {
        try {
            Files.delete(file);
        } catch (IOException ignored) {
            return false;
        }
        deleteEmptyParentFolders(file);
        return true;
    }

    private void deleteEmptyParentFolders(Path file) {
        var parent = file.getParent();
        while (parent != null && deleteIfEmpty(parent)) {
            parent = parent.equals(root) ? null : parent.getParent();
        }
    }

    /**
     * Deletes a folder when it is empty.
     *
     * @return {@code true} when the folder is deleted, {@code false} when it is not empty or cannot be deleted
     */
    private static boolean deleteIfEmpty(Path folder) {
        try (var stream = Files.list(folder)) {
            if (stream.findAny().isEmpty()) {
                Files.delete(folder);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.warn("Failed to check or delete parent directory '{}'.", folder, e);
            return false;
        }
    }

    private FileData copy(String srcName, FileData destData) throws IOException {
        var srcFile = resolveInRoot(srcName);
        if (isIgnored(srcFile)) {
            throw new FileNotFoundException("File '%s' is not found.".formatted(srcName));
        }
        var destFile = resolveInRoot(destData.getName());
        refuseIgnored(destFile);
        Files.createDirectories(destFile.getParent());
        try (var content = Files.newInputStream(srcFile)) {
            replace(content, destFile);
        }
        // The copy keeps the time the source was modified.
        Files.setLastModifiedTime(destFile, Files.getLastModifiedTime(srcFile));
        return getFileData(destFile);
    }

    private static void deleteFile(Path file) throws IOException {
        try {
            Files.delete(file);
        } catch (FileSystemException e) {
            throw locked(e);
        }
    }

    /**
     * Writes the content into the file whole: another program reading the file meets it as it was or as it is
     * written, never half written, and a write that fails leaves the file as it was.
     */
    private static void replace(InputStream content, Path file) throws IOException {
        try {
            FileReplacement.replace(file, content::transferTo);
        } catch (FileSystemException e) {
            throw FileLockedException.lockedOr(e, file);
        }
    }

    /**
     * Tells an existing file the file system refuses to change from the other failures.
     *
     * <p>Such a file is held open by another program, as Excel does on Windows with a workbook it shows, or it is
     * read-only. It is reported as {@link FileLockedException}, naming the file the failure names.
     */
    private static IOException locked(FileSystemException e) {
        return e.getFile() == null ? e : FileLockedException.lockedOr(e, Path.of(e.getFile()));
    }

    @Override
    public void setListener(Listener callback) {
        if (monitor == null) {
            monitor = new ChangesMonitor(new FileChangesMonitor(getRoot(), this::isListed), listenerTimerPeriod);
        }
        monitor.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) {
        var file = resolveInRoot(name);
        try {
            if (Files.exists(file) && !isIgnored(file)) {
                var data = getFileData(file);
                return Collections.singletonList(data);
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve a file '{}'.", file, ex);
        }
        return Collections.emptyList();
    }

    @Override
    public @Nullable FileData checkHistory(String name, String version) throws IOException {
        if (version == null || Objects.equals(version, getVersion(name))) {
            return check(name);
        }
        return null;
    }

    @Override
    public @Nullable FileItem readHistory(String name, String version) throws IOException {
        if (version == null || Objects.equals(version, getVersion(name))) {
            return read(name);
        }
        return null;
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        if (data.getVersion() == null || Objects.equals(data.getVersion(), getVersion(data.getName()))) {
            return delete(data);
        }
        return false;
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (version == null || Objects.equals(version, getVersion(srcName))) {
            return copy(srcName, destData);
        }
        throw new FileNotFoundException("File versions are not supported.");
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setFolders(true).setComments(false).build();
    }

    protected FileData getFileData(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new FileNotFoundException("File '%s' does not exist.".formatted(file));
        }
        var data = new FileData();

        data.setName(nameOf(file));
        data.setModifiedAt(new Date(Files.getLastModifiedTime(file).toMillis()));
        data.setVersion(getVersion(file));
        if (Files.isRegularFile(file)) {
            data.setSize(Files.size(file));
        }
        try {
            var owner = Files.getOwner(file);
            data.setAuthor(new UserInfo(owner.getName()));
        } catch (UnsupportedOperationException ignored) {
            // File system does not support owner attribute
        }
        return data;
    }

    @Override
    public void close() {
        if (monitor != null) {
            monitor.release();
            monitor = null;
        }
    }

    protected void invokeListener() {
        if (monitor != null) {
            // Workaround
            // Can be null because in some cases LocalRepository can be not initialized
            monitor.fireOnChange();
        }
    }

    @Override
    public List<FileData> listFolders(String path) {
        var files = new ArrayList<FileData>();
        var directory = resolveInRoot(path);
        if (Files.isDirectory(directory)) {
            try (var stream = Files.list(directory)) {
                stream.filter(file -> Files.isDirectory(file) && isListed(file)).forEach(file -> {
                    try {
                        files.add(getFileData(file));
                    } catch (Exception ex) {
                        log.warn("Failed to resolve folder '{}'.", directory, ex);
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to list folders in directory '{}'.", directory, e);
            }
        }
        return files;
    }

    // A plain folder keeps no versions; the local repository overrides it and reads the version of the file.
    @SuppressWarnings("java:S1172")
    protected String getVersion(Path file) {
        return null;
    }

    // A plain folder keeps no versions; the local repository overrides it and reads the version of the path.
    @SuppressWarnings("java:S1172")
    protected String getVersion(String path) throws IOException {
        return null;
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        if (version == null || Objects.equals(version, getVersion(path))) {
            return list(path);
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A change of an ignored file is left out, and a full save keeps the ignored files of the folder.
     */
    @Override
    public @Nullable FileData save(FileData folderData,
                                   Iterable<FileItem> files,
                                   ChangesetType changesetType) throws IOException {
        var folder = resolveInRoot(folderData.getName());
        refuseIgnored(folder);
        // Add new files and update existing ones
        var savedFiles = new ArrayList<Path>();
        for (var change : files) {
            var file = resolveInRoot(change.getData().getName());
            if (!isIgnored(file)) {
                apply(change, file);
                savedFiles.add(file);
            }
        }

        if (changesetType == ChangesetType.FULL) {
            removeAbsentFiles(folder, savedFiles);
        }
        var saved = Files.exists(folder) ? getFileData(folder) : null;
        invokeListener();
        return saved;
    }

    /** Writes a file a change gives content, and deletes one it gives none. */
    private static void apply(FileItem change, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        var stream = change.getStream();
        if (stream != null) {
            replace(stream, file);
            var modifiedAt = change.getData().getModifiedAt();
            if (modifiedAt != null) {
                Files.setLastModifiedTime(file, fromMillis(modifiedAt.getTime()));
            }
        } else {
            deleteFile(file);
        }
    }

    private void removeAbsentFiles(Path directory, Collection<Path> toSave) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return isListed(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!toSave.contains(file) && isListed(file)) {
                    deleteAbsentFile(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                // Don't delete the root folder of the change
                if (!dir.equals(directory)) {
                    deleteIfEmpty(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteAbsentFile(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            log.warn("Failed to delete a file: '{}'", file, e);
        }
    }

}
