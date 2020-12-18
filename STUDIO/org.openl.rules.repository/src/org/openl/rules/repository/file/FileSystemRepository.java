package org.openl.rules.repository.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a file-system-based repository. This repository does not support versioning.
 *
 * @author Yury Molchan
 */
public class FileSystemRepository implements FolderRepository, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);

    private File root;
    private int rootPathLength;
    private ChangesMonitor monitor;
    private String id;
    private int listenerTimerPeriod = 10;
    private String name;

    public void setRoot(File root) {
        this.root = root;
    }

    public void setUri(String path) {
        this.root = new File(path);
    }

    public void initialize() {
        try {
            root.mkdirs();
            if (!root.exists() || !root.isDirectory()) {
                throw new IllegalStateException(String.format("Failed to initialize the root directory: [%s]", root));
            }
            String rootPath = root.getCanonicalPath();
            rootPathLength = rootPath.length() + 1;
            monitor = new ChangesMonitor(new FileChangesMonitor(getRoot()), listenerTimerPeriod);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to initialize the root directory: [%s]", root));
        }
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
        LinkedList<FileData> files = new LinkedList<>();
        File directory = new File(root, path);
        listFiles(files, directory);
        return files;
    }

    @Override
    public FileData check(String name) throws IOException {
        File file = new File(root, name);
        if (file.exists()) {
            return getFileData(file);
        }
        return null;
    }

    @Override
    public FileItem read(String name) throws IOException {
        File file = new File(root, name);
        if (file.exists()) {
            FileData data = getFileData(file);
            FileInputStream stream = new FileInputStream(file);
            return new FileItem(data, stream);
        }
        return null;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        FileData saved = write(data, stream);
        invokeListener();
        return saved;
    }

    private FileData write(FileData data, InputStream stream) throws IOException {
        String dataName = data.getName();
        File file = new File(root, dataName);
        file.getParentFile().mkdirs();

        // Close only output stream. This class is not responsible for input stream: stream must be closed in the
        // place where it was created.
        try (FileOutputStream output = new FileOutputStream(file)) {
            IOUtils.copy(stream, output);
        }
        if (data.getModifiedAt() != null) {
            if (!file.setLastModified(data.getModifiedAt().getTime())) {
                LOG.warn("Cannot set modified time to file {}", dataName);
            }
        }
        return getFileData(file);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        List<FileData> result = new ArrayList<>();
        for (FileItem fileItem : fileItems) {
            FileData saved = write(fileItem.getData(), fileItem.getStream());
            result.add(saved);
        }
        invokeListener();
        return result;
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        File file = new File(root, data.getName());
        try {
            FileUtils.delete(file);
        } catch (FileNotFoundException e) {
            return false;
        }
        // Delete empty parent folders
        while (!(file = file.getParentFile()).equals(root) && file.delete()) {
        }
        invokeListener();
        return true;
    }

    private FileData copy(String srcName, FileData destData) throws IOException {
        File srcFile = new File(root, srcName);
        String destName = destData.getName();
        File destFile = new File(root, destName);
        FileUtils.copy(srcFile, destFile);
        return getFileData(destFile);
    }

    @Override
    public void setListener(Listener callback) {
        if (monitor != null) {
            monitor.setListener(callback);
        }
    }

    @Override
    public List<FileData> listHistory(String name) {
        File file = new File(root, name);
        try {
            if (file.exists()) {
                FileData data = getFileData(file);
                return Collections.singletonList(data);
            }
        } catch (Exception ex) {
            LOG.warn("The file cannot be resolved: [{}]", file, ex);
        }
        return Collections.emptyList();
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
    public boolean deleteHistory(FileData data) throws IOException {
        return data.getVersion() == null && delete(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (version == null) {
            return copy(srcName, destData);
        }
        throw new FileNotFoundException("File versions are not supported.");
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).build();
    }

    private void listFiles(Collection<FileData> files, File directory) {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    listFiles(files, file);
                } else {
                    try {
                        FileData data = getFileData(file);
                        files.add(data);
                    } catch (Exception ex) {
                        LOG.warn("The file cannot be resolved in the directory {}.", directory, ex);
                    }
                }
            }
        }
    }

    protected FileData getFileData(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("File [%s] does not exist.", file));
        }
        if (rootPathLength == 0) {
            initialize();
        }
        FileData data = new FileData();
        String canonicalPath = file.getCanonicalPath();
        String relativePath = canonicalPath.substring(rootPathLength);
        String converted = relativePath.replace('\\', '/');
        data.setName(converted);
        long timestamp = file.lastModified();
        Date date = new Date(timestamp);
        data.setModifiedAt(date);
        if (file.isFile()) {
            long size = file.length();
            data.setSize(size);
        }
        return data;
    }

    public File getRoot() {
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
        LinkedList<FileData> files = new LinkedList<>();
        File directory = new File(root, path);
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    try {
                        if (rootPathLength == 0) {
                            initialize();
                        }
                        FileData data = new FileData();
                        String relativePath = file.getCanonicalPath().substring(rootPathLength);
                        data.setName(relativePath.replace('\\', '/'));
                        data.setModifiedAt(new Date(file.lastModified()));
                        data.setVersion(getVersion(file));
                        files.add(data);
                    } catch (Exception ex) {
                        LOG.warn("Folder cannot be resolved in the directory {}.", directory, ex);
                    }
                }
            }
        }
        return files;
    }

    protected String getVersion(File file) {
        return null;
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        if (version == null) {
            return list(path);
        }

        return Collections.emptyList();
    }

    @Override
    public FileData save(FileData folderData,
            Iterable<FileItem> files,
            ChangesetType changesetType) throws IOException {
        // Add new files and update existing ones
        List<File> savedFiles = new ArrayList<>();
        for (FileItem change : files) {
            FileData data = change.getData();
            File file = new File(root, data.getName());
            savedFiles.add(file);
            createParent(file);

            InputStream stream = change.getStream();
            if (stream != null) {
                try (FileOutputStream output = new FileOutputStream(file)) {
                    IOUtils.copy(stream, output);
                }
                if (data.getModifiedAt() != null) {
                    if (!file.setLastModified(data.getModifiedAt().getTime())) {
                        LOG.warn("Cannot set modified time to file {}", data.getName());
                    }
                }
            } else {
                FileUtils.deleteQuietly(file);
            }
        }

        File folder = new File(root, folderData.getName());
        if (changesetType == ChangesetType.FULL) {
            removeAbsentFiles(folder, savedFiles);
        }
        FileData saved = folder.exists() ? getFileData(folder) : null;
        invokeListener();
        return saved;
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException {
        List<FileData> result = new ArrayList<>();
        for (FolderItem folderItem : folderItems) {
            FileData saved = save(folderItem.getData(), folderItem.getFiles(), changesetType);
            result.add(saved);
        }
        return result;
    }

    private void removeAbsentFiles(File directory, Collection<File> toSave) {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (isSkip(file)) {
                    continue;
                }
                if (file.isDirectory()) {
                    removeAbsentFiles(file, toSave);
                } else {
                    if (!toSave.contains(file)) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
    }

    protected boolean isSkip(File file) {
        return false;
    }

    private void createParent(File file) throws FileNotFoundException {
        File parentFile = file.getParentFile();
        if (!parentFile.mkdirs() && !parentFile.exists()) {
            throw new FileNotFoundException("Cannot create the folder " + parentFile.getAbsolutePath());
        }
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

}
