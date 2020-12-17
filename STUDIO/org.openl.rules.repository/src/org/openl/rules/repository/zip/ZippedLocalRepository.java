package org.openl.rules.repository.zip;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Listener;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

/**
 * Read only implementation of Local Repository to support deploying of zip archives as it is from file system without
 * unzipping to temporary directories.</br>
 *
 * <p>
 * NOTE: This repository type doesn't support write actions!
 * </p>
 *
 * @author Vladyslav Pikus
 */
public class ZippedLocalRepository implements FolderRepository, Closeable {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;
    private static final int SPANNED_ARCHIVE_FILE_SIGN = 0x504B0708;

    private final Map<Path, FileSystem> openedFileSystems = new HashMap<>();

    /**
     * Verifies if it's an archive
     * 
     * @see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">List of file signatures</a>
     *
     * @param path path to archive
     * @return {@code true} if it's archive, otherwise {@code false}
     */
    public static boolean zipArchiveFilter(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            int sign = raf.readInt();
            return sign == REGULAR_ARCHIVE_FILE_SIGN || sign == EMPTY_ARCHIVE_FILE_SIGN || sign == SPANNED_ARCHIVE_FILE_SIGN;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String uri;
    private String[] archives;
    private Path root;
    private String id;
    private String name;
    private Map<String, Path> storage;

    public void initialize() {
        File rootFile = new File(uri);
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
        this.root = rootFile.toPath();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException(String.format("Failed to initialize the root directory: [%s].", root));
        }
        final Map<String, Path> localStorage = new HashMap<>();
        if (archives != null && archives.length > 0) {
            for (String archive : archives) {
                if (StringUtils.isBlank(archive)) {
                    continue;
                }
                archive = archive.trim().replace('\\', '/');
                Path pathToArchive = Paths.get(archive);
                boolean exists = Files.exists(pathToArchive);
                boolean isArchive = exists && zipArchiveFilter(pathToArchive);
                if (!pathToArchive.isAbsolute() && (!exists || !isArchive)) {
                    // if path is not absolute, try to resolve it from root folder
                    pathToArchive = root.resolve(archive);
                    exists |= Files.exists(pathToArchive);
                }
                if (!exists) {
                    throw new IllegalStateException(String.format("The path [%s] does not exist.", archive));
                }
                if (!zipArchiveFilter(pathToArchive)) {
                    throw new IllegalStateException(String.format("[%s] is not archive.", archive));
                }
                String archiveName = pathToArchive.getFileName().toString();
                if (localStorage.containsKey(archiveName.toLowerCase())) {
                    throw new IllegalStateException(String.format("An archive name [%s] is duplicated!", archiveName));
                }
                localStorage.put(archiveName.toLowerCase(), pathToArchive);
            }
        }
        if (localStorage.isEmpty()) {
            try {
                Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                        if (attrs.isDirectory() || zipArchiveFilter(p)) {
                            localStorage.put(p.getFileName().toString().toLowerCase(), p);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize a repository", e);
            }
        }
        this.storage = Collections.unmodifiableMap(localStorage);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setArchives(String... archives) {
        this.archives = archives;
    }

    @Override
    public void close() {
        openedFileSystems.values().forEach(IOUtils::closeQuietly);
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        LinkedList<FileData> files = new LinkedList<>();
        CompoundPath resolvedPath = resolvePath(path);
        if (zipArchiveFilter(resolvedPath.getPath())) {
            resolvedPath = new CompoundPath(resolvedPath.getRoot(),
                enterZipArchive(resolvedPath.getPath()),
                resolvedPath.getPath());
        }
        if (!resolvedPath.isDirectory()) {
            return files;
        }
        if (resolvedPath.getPath().equals(root)) {
            for (Path p : storage.values()) {
                listFiles(files, p.getParent(), null, p);
            }
        } else {
            listFiles(files, resolvedPath.getRoot(), resolvedPath.getPathToArchive(), resolvedPath.getPath());
        }
        return files;
    }

    private void listFiles(List<FileData> files, Path root, Path pathToArchive, Path resolvedPath) throws IOException {
        Files.walkFileTree(resolvedPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) throws IOException {
                if (pathToArchive == null && zipArchiveFilter(p)) {
                    try {
                        Path zip = enterZipArchive(p);
                        listFiles(files, root, p, zip);
                    } catch (IOException ignored) {
                        // it's not an archive
                    }
                } else {
                    CompoundPath cp = new CompoundPath(root, p, pathToArchive, attr);
                    FileData data = getFileData(cp);
                    files.add(data);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private FileData getFileData(CompoundPath path) throws IOException {
        FileData data = new FileData();
        data.setName(path.relativize());
        data.setModifiedAt(path.getModifiedAt());
        data.setSize(path.getSize());
        data.setPath(path.getPath());
        return data;
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        List<FileData> files = new LinkedList<>();
        CompoundPath resolvedPath = resolvePath(path);
        if (zipArchiveFilter(resolvedPath.getPath())) {
            resolvedPath = new CompoundPath(resolvedPath.getRoot(),
                enterZipArchive(resolvedPath.getPath()),
                resolvedPath.getPath());
        }
        if (!resolvedPath.isDirectory()) {
            return files;
        }
        final Path walkRoot = resolvedPath.getPath();
        Stream<Path> stream = walkRoot.equals(root) ? storage.values().stream()
                                                    : Files.walk(walkRoot, 1).filter(p -> !walkRoot.equals(p));
        List<Path> found = stream.filter(p -> Files.isDirectory(p) || zipArchiveFilter(p)).collect(Collectors.toList());
        for (Path p : found) {
            CompoundPath cp = new CompoundPath(walkRoot.equals(root) ? p.getParent() : resolvedPath.getRoot(),
                p,
                resolvedPath.getPathToArchive());
            FileData data = new FileData();
            data.setName(cp.relativize());
            data.setModifiedAt(cp.getModifiedAt());
            data.setVersion(String.valueOf(getHashVersion(p)));
            data.setPath(cp.getPath());
            files.add(data);
        }
        return files;
    }

    private CompoundPath resolvePath(String p) throws IOException {
        if (StringUtils.isEmpty(p) || CompoundPath.PATH_SEPARATOR.equals(p)) {
            return new CompoundPath(root, root, null);
        }
        Path resolvedPath = root;
        Path archivePath = null;
        Path localRoot = null;
        String[] folderNames = p.split(CompoundPath.PATH_SEPARATOR);
        int i = 0;
        for (String folderName : folderNames) {
            if (StringUtils.isEmpty(folderName)) {
                continue;
            }
            if (i == 0) {
                resolvedPath = storage.get(folderName.toLowerCase());
                localRoot = resolvedPath != null ? resolvedPath.getParent() : null;
            } else {
                resolvedPath = resolvedPath.resolve(folderName);
            }
            if (resolvedPath == null) {
                throw new IOException(String.format("Unable to resolve the path [%s].", p));
            }
            // don't enter an archive if it's the last token in the path
            if (i < folderNames.length - 1 && archivePath == null && zipArchiveFilter(resolvedPath)) {
                try {
                    Path tmp = resolvedPath;
                    resolvedPath = enterZipArchive(resolvedPath);
                    archivePath = tmp;
                } catch (IOException e) {
                    throw new IOException(String.format("Unable to resolve the path [%s].", p), e);
                }
            }
            i++;
        }
        if ((archivePath != null && !Files.exists(archivePath)) || !Files.exists(resolvedPath)) {
            throw new FileNotFoundException(String.format("File [%s] does not exist.", p));
        }
        return new CompoundPath(localRoot, resolvedPath, archivePath);
    }

    private synchronized Path enterZipArchive(Path path) throws IOException {
        URI jarURI = ZipUtils.toJarURI(path);
        FileSystem fs;
        try {
            fs = FileSystems.getFileSystem(jarURI);
        } catch (FileSystemNotFoundException ignored) {
            fs = FileSystems.newFileSystem(jarURI, Collections.emptyMap());
            openedFileSystems.put(path, fs);
        }
        return fs.getPath(CompoundPath.PATH_SEPARATOR);
    }

    protected int getHashVersion(Path path) throws IOException {
        AtomicInteger hashHolder = new AtomicInteger(1);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) {
                int hash = hashHolder.get();
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

        return Collections.emptyList();
    }

    @Override
    public FileItem read(String name) throws IOException {
        CompoundPath path = resolvePath(name);
        if (!path.isRegularFile()) {
            return null;
        }
        FileData data = getFileData(path);
        InputStream is = Files.newInputStream(path.getPath());
        return new FileItem(data, is);
    }

    @Override
    public FileData check(String name) throws IOException {
        try {
            CompoundPath path = resolvePath(name);
            if (!path.exists()) {
                return null;
            }
            return getFileData(path);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        if (version == null) {
            return check(name);
        }
        return null;
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (version == null) {
            return read(name);
        }
        return null;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).build();
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) {
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
    public void setListener(Listener callback) {

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
