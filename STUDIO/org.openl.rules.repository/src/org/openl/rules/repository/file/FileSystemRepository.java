package org.openl.rules.repository.file;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.FileTime.fromMillis;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.FileUtils;

/**
 * A simple implementation of a file-system-based repository. This repository does not support versioning.
 *
 * @author Yury Molchan
 */
public class FileSystemRepository implements Repository, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);

    private Path root;
    private ChangesMonitor monitor;
    private String id;
    private int listenerTimerPeriod = 10;
    private String name;

    public void setRoot(Path root) {
        this.root = root;
    }

    public void setUri(String path) {
        this.root = Path.of(path);
    }

    public void initialize() {
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
    public List<FileData> list(String path) throws IOException {
        var directory = root.resolve(path);
        if (Files.isDirectory(directory)) {
            try (var stream = Files.walk(directory)) {
                var list = new ArrayList<FileData>();
                stream.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        list.add(getFileData(file));
                    } catch (Exception ex) {
                        LOG.warn("Failed to resolve file '{}' in the directory '{}'.",
                                file.getFileName(),
                                directory,
                                ex);
                    }
                });
                return list;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public FileData check(String name) throws IOException {
        var file = root.resolve(name);
        if (Files.exists(file)) {
            return getFileData(file);
        }
        return null;
    }

    @Override
    public FileItem read(String name) throws IOException {
        var file = root.resolve(name);
        if (Files.exists(file)) {
            var data = getFileData(file);
            var stream = Files.newInputStream(file);
            return new FileItem(data, stream);
        }
        return null;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        var saved = write(data, stream);
        invokeListener();
        return saved;
    }

    private FileData write(FileData data, InputStream stream) throws IOException {
        var dataName = data.getName();
        var file = root.resolve(dataName);
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);

        if (data.getModifiedAt() != null) {
            Files.setLastModifiedTime(file, fromMillis(data.getModifiedAt().getTime()));
        }
        return getFileData(file);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var result = new ArrayList<FileData>();
        for (FileItem fileItem : fileItems) {
            FileData saved = write(fileItem.getData(), fileItem.getStream());
            result.add(saved);
        }
        invokeListener();
        return result;
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        var file = root.resolve(data.getName());
        try {
            FileUtils.delete(file); //FIXME: Use Files.delete() because this API should delete only one file
        } catch (FileNotFoundException e) {
            return false;
        }

        deleteEmptyParentFolders(file);

        invokeListener();
        return true;
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        boolean deleted = false;
        for (var fd : data) {
            var f = root.resolve(fd.getName());
            try {
                Files.delete(f);
                deleted = true;

                deleteEmptyParentFolders(f);
            } catch (IOException ignored) {
                // For example, if the file doesn't exist.
            }
        }
        if (deleted) {
            invokeListener();
        }
        return deleted;
    }

    private void deleteEmptyParentFolders(Path file) {
        var parent = file.getParent();
        while (parent != null) {
            try (var stream = Files.list(parent)) {
                if (stream.findAny().isEmpty()) {
                    Files.delete(parent);
                } else {
                    break;
                }
            } catch (IOException e) {
                LOG.warn("Failed to check or delete parent directory '{}'.", parent, e);
                break;
            }
            parent = parent.equals(root) ? null : parent.getParent();
        }
    }

    private FileData copy(String srcName, FileData destData) throws IOException {
        var srcFile = root.resolve(srcName);
        var destFile = root.resolve(destData.getName());
        Files.createDirectories(destFile.getParent());
        Files.copy(srcFile, destFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
        return getFileData(destFile);
    }

    @Override
    public void setListener(Listener callback) {
        if (monitor == null) {
            monitor = new ChangesMonitor(new FileChangesMonitor(getRoot()), listenerTimerPeriod);
        }
        monitor.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) {
        var file = root.resolve(name);
        try {
            if (Files.exists(file)) {
                FileData data = getFileData(file);
                return Collections.singletonList(data);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to resolve a file '{}'.", file, ex);
        }
        return Collections.emptyList();
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        if (Objects.equals(version, getVersion(name))) {
            return check(name);
        }
        return null;
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (Objects.equals(version, getVersion(name))) {
            return read(name);
        }
        return null;
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        return Objects.equals(data.getVersion(), getVersion(data.getName())) && delete(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (Objects.equals(version, getVersion(srcName))) {
            return copy(srcName, destData);
        }
        throw new FileNotFoundException("File versions are not supported.");
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setFolders(true).build();
    }

    protected FileData getFileData(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new FileNotFoundException(String.format("File '%s' does not exist.", file));
        }
        var data = new FileData();

        var relativePath = root.relativize(file).toString();
        data.setName(relativePath.replace('\\', '/'));
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

    public Path getRoot() {
        return root;
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
        var files = new LinkedList<FileData>();
        var directory = root.resolve(path);
        if (Files.isDirectory(directory)) {
            try (var stream = Files.list(directory)) {
                stream.filter(Files::isDirectory).forEach(file -> {
                    try {
                        files.add(getFileData(file));
                    } catch (Exception ex) {
                        LOG.warn("Failed to resolve folder '{}'.", directory, ex);
                    }
                });
            } catch (IOException e) {
                LOG.warn("Failed to list folders in directory '{}'.", directory, e);
            }
        }
        return files;
    }

    protected String getVersion(Path file) {
        return null;
    }

    protected String getVersion(String path) throws IOException {
        return null;
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        if (Objects.equals(version, getVersion(path))) {
            return list(path);
        }

        return Collections.emptyList();
    }

    @Override
    public FileData save(FileData folderData,
                         Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        // Add new files and update existing ones
        var savedFiles = new ArrayList<Path>();
        for (var change : files) {
            var data = change.getData();
            var file = root.resolve(data.getName());
            savedFiles.add(file);
            Files.createDirectories(file.getParent());

            var stream = change.getStream();
            if (stream != null) {
                Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
                if (data.getModifiedAt() != null) {
                    Files.setLastModifiedTime(file, fromMillis(data.getModifiedAt().getTime()));
                }
            } else {
                Files.delete(file);
            }
        }

        var folder = root.resolve(folderData.getName());
        if (changesetType == ChangesetType.FULL) {
            removeAbsentFiles(folder, savedFiles);
        }
        var saved = Files.exists(folder) ? getFileData(folder) : null;
        invokeListener();
        return saved;
    }

    private void removeAbsentFiles(Path directory, Collection<Path> toSave) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isSkip(file)) {
                    return FileVisitResult.CONTINUE;
                }
                if (!toSave.contains(file)) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        LOG.warn("Failed to delete a file: '{}'", file, e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                if (isSkip(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                // Don't delete the root folder of the change
                if (dir.equals(directory)) {
                    return FileVisitResult.CONTINUE;
                }
                try (var stream = Files.list(dir)) {
                    if (stream.findAny().isEmpty()) {
                        Files.delete(dir);
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to delete an empty directory: '{}'", dir, e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected boolean isSkip(Path file) {
        return false;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

}
