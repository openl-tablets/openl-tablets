package org.openl.rules.repository.file;

import java.io.*;
import java.util.*;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a file-system-based repository. This repository
 * does not support versioning.
 *
 * @author Yury Molchan
 */
public class FileSystemRepository implements FolderRepository, RRepositoryFactory, Closeable {
    private final Logger log = LoggerFactory.getLogger(FileSystemRepository.class);

    private File root;
    private int rootPathLength;
    private ChangesMonitor monitor;


    public void setRoot(File root) {
        this.root = root;
    }

    public void setUri(String path) {
        this.root = new File(path);
    }

    public void initialize() throws RRepositoryException {
        try {
            init();
        } catch (IOException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    private void init() throws IOException {
        root.mkdirs();
        if (!root.exists() || !root.isDirectory()) {
            throw new IOException("Failed to initialize the root directory: [" + root + "]");
        }
        String rootPath = root.getCanonicalPath();
        rootPathLength = rootPath.length() + 1;
        monitor = new ChangesMonitor(new FileChangesMonitor(getRoot()), 10);
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
        String name = data.getName();
        File file = new File(root, name);
        file.getParentFile().mkdirs();
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            IOUtils.copy(stream, output);
        } finally {
            // Close only output stream. This class isn't responsible for input stream: stream must be closed in the
            // place where it was created.
            IOUtils.closeQuietly(output);
        }
        return getFileData(file);
    }

    @Override
    public boolean delete(FileData data) {
        File file = new File(root, data.getName());
        boolean deleted;
        try {
            FileUtils.delete(file);
            deleted = true;
        } catch (IOException e) {
            deleted = false;
        }
        // Delete empty parent folders
        while (!(file = file.getParentFile()).equals(root) && file.delete());
        return deleted;
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
            log.warn("The file cannot be resolved: [{}]", file, ex);
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
    public boolean deleteHistory(FileData data) {
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
        return new Features(this);
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
                        log.warn("The file cannot be resolved in the directory {}.", directory, ex);
                    }
                }
            }
        }
    }

    private FileData getFileData(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File [" + file + "] does not exist.");
        }
        if (rootPathLength == 0) {
            init();
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
                            init();
                        }
                        FileData data = new FileData();
                        String relativePath = file.getCanonicalPath().substring(rootPathLength);
                        data.setName(relativePath.replace('\\', '/'));
                        data.setModifiedAt(new Date(file.lastModified()));
                        files.add(data);
                    } catch (Exception ex) {
                        log.warn("Folder cannot be resolved in the directory {}.", directory, ex);
                    }
                }
            }
        }
        return files;
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        if (version == null) {
            return list(path);
        }

        return Collections.emptyList();
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileChange> files) throws IOException {
        // Add new files and update existing ones
        List<File> savedFiles = new ArrayList<>();
        for (FileChange change : files) {
            File file = new File(root, change.getName());
            savedFiles.add(file);
            createParent(file);

            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file);
                IOUtils.copy(change.getStream(), output);
            } finally {
                // Close only output stream. This class isn't responsible for input stream: stream must be closed in the
                // place where it was created.
                IOUtils.closeQuietly(output);
            }
        }

        File folder = new File(root, folderData.getName());
        removeAbsentFiles(folder, savedFiles);

        return folder.exists() ? getFileData(folder) : null;
    }

    private void removeAbsentFiles(File directory, Collection<File> toSave) {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
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

    private void createParent(File file) throws FileNotFoundException {
        File parentFile = file.getParentFile();
        if (!parentFile.mkdirs() && !parentFile.exists()) {
            throw new FileNotFoundException("Can't create the folder " + parentFile.getAbsolutePath());
        }
    }
}
