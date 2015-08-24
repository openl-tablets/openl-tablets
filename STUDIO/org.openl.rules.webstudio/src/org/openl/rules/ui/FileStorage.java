package org.openl.rules.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * File storage.
 *
 * @author Andrei Astrouski
 */
public class FileStorage {

    private final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final String storagePath;
    private final boolean versioning;

    public FileStorage(String storagePath) {
        this(storagePath, false);
    }

    public FileStorage(String storagePath, boolean versioning) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Illegal storage path");
        }
        this.storagePath = storagePath;
        this.versioning = versioning;
    }

    public synchronized int getSize() {
        return list().size();
    }

    public synchronized boolean add(File srcFile) {
        if (srcFile == null) {
            throw new IllegalArgumentException();
        }

        boolean added = false;

        long currentDate = new Date().getTime();

        String innerFilePath = srcFile.getName();
        if (versioning) {
            innerFilePath = currentDate + File.separator + innerFilePath;
        }

        String destFilePath = getFullFilePath(innerFilePath);
        File destFile = new File(destFilePath);

        try {
            FileUtils.copyFile(srcFile, destFile);
            destFile.setLastModified(currentDate);
            added = true;
        } catch (Exception e) {
            log.error("Can't add file {}", srcFile.getName(), e);
        }

        return added;
    }

    public synchronized boolean delete(String fileName) {
        return delete(new NameFileFilter(fileName));
    }

    public synchronized boolean delete(String[] fileNames) {
        return delete(new NameFileFilter(fileNames));
    }

    public synchronized void delete(int count) {
        File[] files = new File(storagePath).listFiles();
        if (files != null && files.length > count) {
            for (int i = 0; i < files.length - count; i++) {
                File file = files[i];
                try {
                    FileUtils.deleteDirectory(file);
                } catch (Exception e) {
                    log.error("Can't delete folder {}", file.getName(), e);
                }
            }
        }
    }

    public synchronized boolean delete(IOFileFilter fileFilter) {
        Collection<File> filesToDelete = list(fileFilter);

        boolean deleted = false;

        for (File fileToDelete : filesToDelete) {
            if (versioning && !fileToDelete.getParent().equals(storagePath)) {
                fileToDelete = fileToDelete.getParentFile();
            }
            if (!delete(fileToDelete)) {
                deleted = false;
                break;
            }
        }

        return deleted;
    }

    private boolean delete(File file) {
        boolean deleted = false;
        try {
            FileUtils.forceDelete(file);
            deleted = true;
        } catch (Exception e) {
            log.error("Can't delete file {}", file.getName(), e);
        }
        return deleted;
    }

    public synchronized void clean() {
        File storageDir = getStorageDir();
        if (storageDir.exists()) {
            try {
                FileUtils.cleanDirectory(storageDir);
            } catch (Exception e) {
                log.error("Can't clean folder {}", storageDir.getName(), e);
            }
        }
    }

    public synchronized Collection<File> list() {
        return list(TrueFileFilter.TRUE);
    }

    public synchronized Collection<File> list(String fileName) {
        return list(new NameFileFilter(fileName));
    }

    public synchronized Collection<File> list(String[] fileNames) {
        return list(new NameFileFilter(fileNames));
    }

    public synchronized Collection<File> list(IOFileFilter fileFilter) {
        File storageDir = getStorageDir();
        if (storageDir.exists()) {
            return FileUtils.listFiles(storageDir, fileFilter, TrueFileFilter.TRUE);
        }
        return Collections.emptyList();
    }

    private File getStorageDir() {
        return new File(storagePath);
    }

    private String getFullFilePath(String innerPath) {
        return storagePath + File.separator + innerPath;
    }

}
