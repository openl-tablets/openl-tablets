package org.openl.rules.project.impl.local;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openl.rules.repository.api.*;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class LocalRepository implements Repository, FolderRepository {
    private final File location;
    private final ModificationHandler modificationHandler;

    public LocalRepository(File location) {
        this.location = location;
        this.modificationHandler = new DummyModificationHandler();
    }

    public LocalRepository(File location, ModificationHandler modificationHandler) {
        this.location = location;
        this.modificationHandler = modificationHandler;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        File file = new File(location, path);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        if (file.isFile()) {
            return Collections.singletonList(createFileData(file));
        } else {
            List<File> allFiles = new ArrayList<File>();
            getFilesRecursively(allFiles, file.listFiles());


            List<FileData> result = new ArrayList<FileData>();
            for (File f : allFiles) {
                result.add(createFileData(f));
            }
            return result;
        }
    }

    @Override
    public FileData check(String name) throws IOException {
        File file = new File(location, name);
        return createFileData(file);
    }

    @Override
    public FileItem read(String name) {
        File file = new File(location, name);
        try {
            return new FileItem(createFileData(file), new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) {
        File file = new File(location, data.getName());
        OutputStream os = null;
        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IllegalStateException("Can't create the dirs for path " + location.getAbsolutePath());
                }
            }
            os = new FileOutputStream(file);
            IOUtils.copy(stream, os);
            os.close();
            modificationHandler.notifyModified(data.getName());
            return createFileData(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public boolean delete(String path) {
        File file = new File(location, path);
        boolean exists = file.exists();
        if (!exists) {
            return false;
        }

        FileUtils.deleteQuietly(file);
        modificationHandler.notifyModified(path);
        return !file.exists();
    }

    @Override
    public FileData copy(String srcPath, String destPath) {
        return null;
    }

    @Override
    public FileData rename(String path, String destination) {
        File file = new File(location, path);
        File dest = new File(location, destination);
        if (!file.renameTo(dest)) {
            throw new IllegalStateException("Can't rename");
        }
        modificationHandler.notifyModified(destination);

        return createFileData(dest);
    }

    @Override
    public void setListener(Listener callback) {
        // TODO Implement
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        return null;
    }

    @Override
    public FileItem readHistory(String name, String version) {
        return null;
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        return delete(name);
    }

    @Override
    public FileData copyHistory(String srcName, String destName, String version) {
        return null;
    }

    public File getLocation() {
        return location;
    }

    private void getFilesRecursively(List<File> allFiles, File[] files) {
        for (File f : files) {
            if (!f.exists()) {
                continue;
            }
            if (f.isFile()) {
                allFiles.add(f);
            } else {
                if (f.getName().equals(FolderHelper.PROPERTIES_FOLDER)) {
                    /**
                     * Marker file must be hidden
                     * @see {@link org.openl.rules.workspace.lw.impl.LocalProjectModificationHandler#MARKER_FILE_NAME}
                     */
                    continue;
                }
                getFilesRecursively(allFiles, f.listFiles());
            }
        }
    }

    private FileData createFileData(File file) {
        FileData fileData = new FileData();
        String absolutePath = file.getAbsolutePath();
        String locationAbsolutePath = location.getAbsolutePath();
        String name = absolutePath.substring(locationAbsolutePath.length() + 1).replace(File.separatorChar, '/');

        fileData.setName(name);
        fileData.setSize(file.length());
        fileData.setModifiedAt(new Date(file.lastModified()));

        return fileData;
    }

    public void notifyModified(String path) {
        modificationHandler.notifyModified(path);
    }

    public void clearModifyStatus(String path) {
        modificationHandler.clearModifyStatus(path);
    }

    public boolean isModified(String path) {
        return modificationHandler.isModified(path);
    }

    /**
     * When there is no need in modification tracking
     */
    private static class DummyModificationHandler implements ModificationHandler {
        @Override
        public void notifyModified(String path) {
        }

        @Override
        public boolean isModified(String path) {
            return false;
        }

        @Override
        public void clearModifyStatus(String path) {
        }
    }
}
