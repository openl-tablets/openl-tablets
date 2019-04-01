package org.openl.rules.ui;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File storage.
 *
 * @author Andrei Astrouski
 */
public class FileStorage {

    private final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final String storagePath;

    public FileStorage(String storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Illegal storage path");
        }
        this.storagePath = storagePath;
    }

    public synchronized void add(File srcFile) {
        if (srcFile == null) {
            throw new IllegalArgumentException();
        }

        long currentDate = System.currentTimeMillis();

        String destFilePath = currentDate + File.separator + srcFile.getName();
        File destFile = new File(storagePath, destFilePath);

        try {
            FileUtils.copyFile(srcFile, destFile);
            destFile.setLastModified(currentDate);
        } catch (Exception e) {
            log.error("Can't add file {}", srcFile.getName(), e);
        }
    }

    public synchronized void delete(int count) {
        File[] files = new File(storagePath).listFiles();
        // Initial version must always exist
        if (files != null && files.length > count + 1) {
            Arrays.sort(files);
            // Don't delete initial version
            for (int i = 1; i < files.length - count; i++) {
                File file = files[i];
                try {
                    FileUtils.deleteDirectory(file);
                } catch (Exception e) {
                    log.error("Can't delete folder {}", file.getName(), e);
                }
            }
        }
    }

    public synchronized Collection<File> list(IOFileFilter fileFilter) {
        File storageDir = new File(storagePath);
        if (storageDir.exists()) {
            return FileUtils.listFiles(storageDir, fileFilter, TrueFileFilter.TRUE);
        }
        return Collections.emptyList();
    }
}
