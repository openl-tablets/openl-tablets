package org.openl.rules.repository.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
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

    private final Map<Path, FileSystem> openedFileSystems = new HashMap<>();

    private static final String PATH_SEPARATOR = "/";
    private static final String ZIP_EXT = "zip";

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
    private Path root;
    private String id;
    private String name;

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
            throw new IOException(String.format("Failed to initialize the root directory: [%s]", root));
        }
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

    @Override
    public void close() {
        openedFileSystems.values().forEach(IOUtils::closeQuietly);
    }


    @Override
    public List<FileData> list(String path) throws IOException {
        LinkedList<FileData> files = new LinkedList<>();
        Pair<Path, Path> paths = resolvePath(path);
        Path resolvedPath = paths.getValue();
        Path pathToArchive = paths.getKey();
        if (!Files.isDirectory(resolvedPath)) {
            return files;
        }
        listFiles(files, pathToArchive, resolvedPath);
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
                    FileData data = getFileData(pathToArchive, p, attr);
                    files.add(data);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private FileData getFileData(Path pathToArchive, Path pathToFile, BasicFileAttributes attr) {
        FileData data = new FileData();
        data.setName(resolveName(pathToArchive, pathToFile));
        data.setModifiedAt(new Date(attr.lastModifiedTime().toMillis()));
        data.setSize(attr.size());
        return data;
    }


    @Override
    public List<FileData> listFolders(String path) throws IOException {
        List<FileData> files = new LinkedList<>();
        Pair<Path, Path> paths = resolvePath(path);
        Path pathToArchive = paths.getKey();
        Path resolvedPath = paths.getValue();
        if (!Files.isDirectory(resolvedPath)) {
            return files;
        }
        List<Path> found = Files.walk(resolvedPath, 1)
                .filter(p -> !resolvedPath.equals(p))
                .filter(p -> Files.isDirectory(p) || zipArchiveFilter(p))
                .collect(Collectors.toList());
        for (Path p : found) {
            FileData data = new FileData();
            data.setName(resolveName(pathToArchive, p));
            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
            data.setModifiedAt(new Date(attr.lastModifiedTime().toMillis()));
            data.setVersion(String.valueOf(getHashVersion(p)));
            files.add(data);
        }
        return files;
    }

    private String resolveName(Path pathToArchive, Path pathToFile) {
        String name;
        if (pathToArchive == null) {
            name = root.relativize(pathToFile)
                    .toString();
        } else {
            String pathInArchive = pathToFile.toString();
            if (pathInArchive.charAt(0) == PATH_SEPARATOR.charAt(0)) {
                pathInArchive = pathInArchive.substring(1);
            }
            name = root.relativize(pathToArchive)
                    .resolve(pathInArchive)
                    .toString();
        }
        return name.replace('\\', PATH_SEPARATOR.charAt(0));
    }

    private Pair<Path, Path> resolvePath(String p) throws IOException {
        Path resolvedPath = root;
        Path archivePath = null;
        String[] folderNames = p.split(PATH_SEPARATOR);
        for (String folderName : folderNames) {
            resolvedPath = resolvedPath.resolve(folderName);
            if (archivePath == null && zipArchiveFilter(resolvedPath)) {
                try {
                    Path tmp = resolvedPath;
                    resolvedPath = enterZipArchive(resolvedPath);
                    archivePath = tmp;
                } catch (IOException e) {
                    throw new IOException(String.format("Unable to resolve the path [%s].", p), e);
                }
            }
        }
        if ((archivePath != null && !Files.exists(archivePath)) || !Files.exists(resolvedPath)) {
            throw new FileNotFoundException(String.format("File [%s] does not exist.", p));
        }
        return Pair.of(archivePath, resolvedPath);
    }

    private synchronized Path enterZipArchive(Path path) throws IOException {
        FileSystem fileSystem = openedFileSystems.get(path);
        if (fileSystem == null) {
            fileSystem = FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
            openedFileSystems.put(path, fileSystem);
        }
        return fileSystem.getPath(PATH_SEPARATOR);
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
        Pair<Path, Path> paths = resolvePath(name);
        Path resolvedPath = paths.getValue();
        Path pathToArchive = paths.getKey();
        BasicFileAttributes attr = Files.readAttributes(resolvedPath, BasicFileAttributes.class);
        if (!attr.isRegularFile()) {
            return null;
        }
        FileData data = getFileData(pathToArchive, resolvedPath, attr);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(resolvedPath, baos);
        return new FileItem(data, new ByteArrayInputStream(baos.toByteArray()));
    }

    @Override
    public FileData check(String name) throws IOException {
        Pair<Path, Path> paths = resolvePath(name);
        Path pathToArchive = paths.getKey();
        Path resolvedPath = paths.getValue();
        BasicFileAttributes attr = Files.readAttributes(resolvedPath, BasicFileAttributes.class);
        if (!attr.isRegularFile()) {
            return null;
        }
        return getFileData(pathToArchive, resolvedPath, attr);
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
