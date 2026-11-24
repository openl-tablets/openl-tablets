package org.openl.rules.repository.zip;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

/**
 * Read only repository of the folders and the archives of the file system, which reads an archive without unzipping it
 * to a temporary folder.
 *
 * <p>The top level of the repository lists the folders and the archives it was created with, each under its own
 * name. The names are case-insensitive. An archive is entered when a path goes into it.
 *
 * <p>NOTE: This repository type doesn't support write actions!
 *
 * @author Vladyslav Pikus
 */
abstract class AbstractArchiveRepository implements Repository, Closeable {

    private final Map<Path, FileSystem> openedFileSystems = new HashMap<>();
    private final Map<String, Path> storage;
    private final Map<Path, String> pathAliases;
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String name;

    /**
     * @param storage the folders and the archives of the top level by their names
     * @throws IllegalStateException when two names differ only in case or two names stand for the same path
     */
    AbstractArchiveRepository(Map<String, Path> storage) {
        var localStorage = new HashMap<String, Path>();
        var localPathAliases = new HashMap<Path, String>();
        storage.forEach((k, v) -> {
            if (localStorage.put(keyOf(k), v) != null) {
                throw new IllegalStateException("The resource with name '%s' already exits.".formatted(k));
            }
            if (localPathAliases.put(v, k) != null) {
                throw new IllegalStateException("The resource with path '%s' already exits.".formatted(v));
            }
        });
        this.storage = Collections.unmodifiableMap(localStorage);
        this.pathAliases = Collections.unmodifiableMap(localPathAliases);
    }

    /**
     * The key a name of the top level is looked up by: names differing only in letter case stand for the same folder
     * or archive.
     */
    static String keyOf(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public void close() {
        openedFileSystems.values().forEach(IOUtils::closeQuietly);
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        var files = new ArrayList<FileData>();
        if (isTopLevel(path)) {
            for (Path p : storage.values()) {
                collectFiles(files, p, null, p);
            }
        } else {
            var folder = resolveFolder(path);
            if (folder != null) {
                collectFiles(files, folder.getTop(), folder.getPathToArchive(), folder.getPath());
            }
        }
        return files;
    }

    private void collectFiles(List<FileData> files,
                              Path top,
                              @Nullable Path pathToArchive,
                              Path resolvedPath) throws IOException {
        Files.walkFileTree(resolvedPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) throws IOException {
                if (pathToArchive == null && ZipUtils.isArchive(p)) {
                    try {
                        collectFiles(files, top, p, enterZipArchive(p));
                    } catch (IOException ignored) {
                        // it's not an archive
                    }
                } else {
                    files.add(getFileData(new CompoundPath(top, p, pathToArchive, attr)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private FileData getFileData(CompoundPath path) throws IOException {
        var data = new FileData();
        data.setName(path.relativize(pathAliases::get));
        data.setModifiedAt(path.getModifiedAt());
        data.setSize(path.getSize());
        data.setPath(path.getPath());
        return data;
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        List<CompoundPath> found;
        if (isTopLevel(path)) {
            found = storage.values()
                    .stream()
                    .filter(AbstractArchiveRepository::isFolder)
                    .map(p -> new CompoundPath(p, p, null))
                    .toList();
        } else {
            var folder = resolveFolder(path);
            if (folder == null) {
                return List.of();
            }
            var walkRoot = folder.getPath();
            try (Stream<Path> stream = Files.walk(walkRoot, 1)) {
                found = stream.filter(p -> !walkRoot.equals(p) && isFolder(p))
                        .map(p -> new CompoundPath(folder.getTop(), p, folder.getPathToArchive()))
                        .toList();
            }
        }
        var folders = new ArrayList<FileData>();
        for (var cp : found) {
            var data = new FileData();
            data.setName(cp.relativize(pathAliases::get));
            data.setModifiedAt(cp.getModifiedAt());
            data.setVersion(String.valueOf(getHashVersion(cp.getPath())));
            data.setPath(cp.getPath());
            folders.add(data);
        }
        return folders;
    }

    private static boolean isFolder(Path path) {
        return Files.isDirectory(path) || ZipUtils.isArchive(path);
    }

    private static boolean isTopLevel(String path) {
        return StringUtils.isEmpty(path) || CompoundPath.PATH_SEPARATOR.equals(path);
    }

    /**
     * Resolves a path that is not the top level as a folder, entering the archive the path names.
     *
     * @return {@code null} when the path names a file
     */
    private @Nullable CompoundPath resolveFolder(String path) throws IOException {
        var resolvedPath = resolvePath(path);
        if (ZipUtils.isArchive(resolvedPath.getPath())) {
            resolvedPath = new CompoundPath(resolvedPath.getTop(),
                    enterZipArchive(resolvedPath.getPath()),
                    resolvedPath.getPath());
        }
        return resolvedPath.isDirectory() ? resolvedPath : null;
    }

    private CompoundPath resolvePath(String p) throws IOException {
        Path resolvedPath = null;
        Path archivePath = null;
        Path top = null;
        Path path = Path.of(p);
        var i = 0;
        for (Path f : path) {
            final var folderName = f.toString();
            if (StringUtils.isEmpty(folderName)) {
                continue;
            }
            if (".".equals(folderName) || "..".equals(folderName)) {
                // a path never leaves the folder or the archive it names
                throw new IOException("Unable to resolve the path [%s].".formatted(p));
            }
            resolvedPath = i == 0 ? storage.get(keyOf(folderName)) : resolvedPath.resolve(folderName);
            if (resolvedPath == null) {
                throw new IOException("Unable to resolve the path [%s].".formatted(p));
            }
            if (i == 0) {
                top = resolvedPath;
            }
            // don't enter an archive if it's the last token in the path
            if (i < path.getNameCount() - 1 && archivePath == null && ZipUtils.isArchive(resolvedPath)) {
                archivePath = resolvedPath;
                resolvedPath = enterZipArchive(resolvedPath, p);
            }
            i++;
        }
        checkExists(archivePath, resolvedPath, p);
        return new CompoundPath(top, resolvedPath, archivePath);
    }

    private static void checkExists(@Nullable Path archivePath,
                                    Path resolvedPath,
                                    String p) throws FileNotFoundException {
        if ((archivePath != null && !Files.exists(archivePath)) || !Files.exists(resolvedPath)) {
            throw new FileNotFoundException("File [%s] does not exist.".formatted(p));
        }
    }

    /**
     * Enters the archive met on the way to the given path.
     *
     * @throws IOException when the archive cannot be opened, telling the path that cannot be resolved
     */
    private Path enterZipArchive(Path archive, String p) throws IOException {
        try {
            return enterZipArchive(archive);
        } catch (IOException e) {
            throw new IOException("Unable to resolve the path [%s].".formatted(p), e);
        }
    }

    private synchronized Path enterZipArchive(Path path) throws IOException {
        var jarURI = ZipUtils.toJarURI(path);
        FileSystem fs;
        try {
            fs = FileSystems.getFileSystem(jarURI);
        } catch (FileSystemNotFoundException ignored) {
            fs = FileSystems.newFileSystem(jarURI, Map.of());
            openedFileSystems.put(path, fs);
        }
        return fs.getPath(CompoundPath.PATH_SEPARATOR);
    }

    private static int getHashVersion(Path path) throws IOException {
        var hashHolder = new AtomicInteger(1);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) {
                var hash = hashHolder.get();
                hash = 31 * hash + Objects.hash(p.getFileName().toString(), attr.lastModifiedTime(), attr.size());
                hashHolder.set(hash);
                return FileVisitResult.CONTINUE;
            }
        });
        return hashHolder.get();
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        if (version == null) {
            return list(path);
        }

        return List.of();
    }

    @Override
    public @Nullable FileItem read(String name) throws IOException {
        if (isTopLevel(name)) {
            return null;
        }
        var path = resolvePath(name);
        if (!path.isRegularFile()) {
            return null;
        }
        var data = getFileData(path);
        var is = Files.newInputStream(path.getPath());
        return new FileItem(data, is);
    }

    @Override
    public @Nullable FileData check(String name) throws IOException {
        if (isTopLevel(name)) {
            return null;
        }
        try {
            var path = resolvePath(name);
            if (!path.exists()) {
                return null;
            }
            return getFileData(path);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public @Nullable FileData checkHistory(String name, String version) throws IOException {
        if (version == null) {
            return check(name);
        }
        return null;
    }

    @Override
    public @Nullable FileItem readHistory(String name, String version) throws IOException {
        if (version == null) {
            return read(name);
        }
        return null;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).setFolders(true).build();
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData save(FileData data, InputStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(FileData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(List<FileData> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setListener(Listener callback) {
        // nothing changes in a read only repository
    }

    @Override
    public List<FileData> listHistory(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteHistory(FileData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) {
        throw new UnsupportedOperationException();
    }
}
