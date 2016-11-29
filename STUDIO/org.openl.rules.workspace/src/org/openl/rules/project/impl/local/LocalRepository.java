package org.openl.rules.project.impl.local;

import java.io.*;
import java.util.*;

import org.openl.rules.repository.api.*;
import org.openl.rules.repository.file.FileRepository;

public class LocalRepository extends FileRepository implements FolderRepository {
    private final ModificationHandler modificationHandler;

    public LocalRepository(File location) {
        super(location);
        this.modificationHandler = new DummyModificationHandler();
        try {
            initialize();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public LocalRepository(File location, ModificationHandler modificationHandler) {
        super(location);
        this.modificationHandler = modificationHandler;
        try {
            initialize();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        List<FileData> list = super.list(path);

        for (Iterator<FileData> iterator = list.iterator(); iterator.hasNext(); ) {
            FileData fileData = iterator.next();
            if (modificationHandler.isMarkerFile(fileData.getName())) {
                // Marker file must be hidden
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        FileData fileData = super.save(data, stream);
        modificationHandler.notifyModified(data.getName());
        return fileData;
    }

    @Override
    public boolean delete(String name) {
        boolean deleted = super.delete(name);
        if (deleted) {
            modificationHandler.notifyModified(name);
        }
        return deleted;
    }

    @Override
    public FileData copy(String srcPath, FileData destData) throws IOException {
        FileData fileData = super.copy(srcPath, destData);
        modificationHandler.notifyModified(destData.getName());
        return fileData;
    }

    @Override
    public FileData rename(String path, FileData destData) throws IOException {
        FileData fileData = super.rename(path, destData);
        modificationHandler.notifyModified(destData.getName());
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

        @Override
        public boolean isMarkerFile(String name) {
            return false;
        }
    }
}
