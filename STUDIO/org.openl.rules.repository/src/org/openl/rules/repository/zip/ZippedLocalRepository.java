package org.openl.rules.repository.zip;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Read only implementation of Local Repository to support deploying of zip archives as it is from file system without
 * unzipping to temporary directories.</br>
 *
 * <p>
 *     NOTE: This repository type doesn't support write actions!
 * </p>
 *
 * @author Vladyslav Pikus
 */
public class ZippedLocalRepository implements FolderRepository, RRepositoryFactory, Closeable  {

    private static final Pattern PATH_SEP = Pattern.compile("[\\\\/]");
    private static final String ZIP_EXT = "zip";

    private final Map<Path, FileSystem> openedFileSystems = new HashMap<>();

    private static boolean zipArchiveFilter(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx > -1) {
            String extension = name.substring(idx + 1);
            return ZIP_EXT.equals(extension);
        }
        return false;
    }

    private String uri;
    private String[] archives;
    private Path root;
    private String id;
    private String name;
    private Map<String, Path> storage;

    @Override
    public void initialize() throws RRepositoryException {
        try {
            init();
        } catch (IOException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    private void init() throws IOException {
        File rootFile = new File(uri);
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
        this.root = rootFile.toPath();
        if (!Files.isDirectory(root)) {
            throw new IOException(String.format("Failed to initialize the root directory: [%s].", root));
        }
        final Map<String, Path> localStorage = new HashMap<>();
        if (archives != null && archives.length > 0) {
            for (String archiveName : archives) {
                if (StringUtils.isBlank(archiveName)) {
                    throw new IOException("An archive name cannot be blank!");
                }
                if (localStorage.containsKey(archiveName.toLowerCase())) {
                    throw new IOException(String.format("An archive name [%s] is duplicated!", archiveName));
                }
                if (PATH_SEP.matcher(archiveName).find()) {
                    throw new IOException(String.format("An archive name [%s] must not contain characters of path separator!", archiveName));
                }
                Path pathToArchive = root.resolve(archiveName);
                if (!Files.exists(pathToArchive)) {
                    throw new IOException(String.format("The path [%s] does not exist.", archiveName));
                }
                if (!zipArchiveFilter(pathToArchive)) {
                    throw new IOException(String.format("[%s] is not archive.", archiveName));
                }
                localStorage.put(archiveName.toLowerCase(), pathToArchive);
            }
        }
        if (localStorage.isEmpty()) {
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    if (attrs.isDirectory() || zipArchiveFilter(p)) {
                        localStorage.put(p.getFileName().toString().toLowerCase(), p);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
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
        if (!resolvedPath.isDirectory()) {
            return files;
        }
        if (resolvedPath.getPath().equals(root)) {
            for (Path p : storage.values()) {
                listFiles(files, null, p);
            }
        } else {
            listFiles(files, resolvedPath.getPathToArchive(), resolvedPath.getPath());
        }
        return files;
    }

    private void listFiles(List<FileData> files, Path pathToArchive, Path resolvedPath) throws IOException {
        Files.walkFileTree(resolvedPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) throws IOException {
                if (pathToArchive == null && zipArchiveFilter(p)) {
                    try {
                        Path zip = enterZipArchive(p);
                        listFiles(files, p, zip);
                    } catch (IOException ignored) {
                        //it's not an archive
                    }
                } else {
                    CompoundPath cp = new CompoundPath(p, pathToArchive, attr);
                    FileData data = getFileData(cp);
                    files.add(data);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private FileData getFileData(CompoundPath path) throws IOException {
        FileData data = new FileData();
        data.setName(path.relativize(root));
        data.setModifiedAt(path.getModifiedAt());
        data.setSize(path.getSize());
        return data;
    }


    @Override
    public List<FileData> listFolders(String path) throws IOException {
        List<FileData> files = new LinkedList<>();
        final CompoundPath resolvedPath = resolvePath(path);
        if (!resolvedPath.isDirectory()) {
            return files;
        }
        Stream<Path> stream = resolvedPath.getPath().equals(root) ? storage.values().stream()
                : Files.walk(resolvedPath.getPath(), 1).filter(p -> !resolvedPath.getPath().equals(p));
        List<Path> found = stream.filter(p -> Files.isDirectory(p) || zipArchiveFilter(p))
                .collect(Collectors.toList());
        for (Path p : found) {
            CompoundPath cp = new CompoundPath(p, resolvedPath.getPathToArchive());
            FileData data = new FileData();
            data.setName(cp.relativize(root));
            data.setModifiedAt(cp.getModifiedAt());
            data.setVersion(String.valueOf(getHashVersion(p)));
            files.add(data);
        }
        return files;
    }

    private CompoundPath resolvePath(String p) throws IOException {
        if (StringUtils.isEmpty(p) || CompoundPath.PATH_SEPARATOR.equals(p)) {
            return new CompoundPath(root, null);
        }
        Path resolvedPath = root;
        Path archivePath = null;
        String[] folderNames = p.split(CompoundPath.PATH_SEPARATOR);
        int i = 0;
        for (String folderName : folderNames) {
            if (StringUtils.isEmpty(folderName)) {
                continue;
            }
            resolvedPath = i == 0 ? storage.get(folderName.toLowerCase()) : resolvedPath.resolve(folderName);
            if (resolvedPath == null) {
                throw new IOException(String.format("Unable to resolve the path [%s].", p));
            }
            if (archivePath == null && zipArchiveFilter(resolvedPath)) {
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
        return new CompoundPath(resolvedPath, archivePath);
    }

    private synchronized Path enterZipArchive(Path path) throws IOException {
        FileSystem fileSystem = openedFileSystems.get(path);
        if (fileSystem == null) {
            fileSystem = FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
            openedFileSystems.put(path, fileSystem);
        }
        return fileSystem.getPath(CompoundPath.PATH_SEPARATOR);
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
        CompoundPath path = resolvePath(name);
        if (!path.isRegularFile()) {
            return null;
        }
        return getFileData(path);
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
