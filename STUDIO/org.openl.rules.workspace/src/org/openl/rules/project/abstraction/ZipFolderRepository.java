package org.openl.rules.project.abstraction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.api.*;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Treat zip files as separate repositories
 */
class ZipFolderRepository implements Repository {
    private final Logger log = LoggerFactory.getLogger(ZipFolderRepository.class);

    private static final int GET_FILE_RETRY_DELAY_S = 1;
    private static final int GET_FILE_RETRY_ATTEMPTS = 5;

    private final Repository delegate;
    private final String zipPath;
    private final String version;

    ZipFolderRepository(Repository delegate, String zipPath, String version) {
        this.delegate = delegate;
        this.zipPath = zipPath;
        this.version = version;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        String artefactPath = path.substring(zipPath.length() + 1);

        List<FileData> result = new ArrayList<>();

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = getZipInputStream();
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().startsWith(artefactPath)) {
                    result.add(createFileData(entry));
                }
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        return result;
    }

    @Override
    public FileData check(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileItem read(String name) throws IOException {
        String artefactName = name.substring(zipPath.length() + 1);

        ZipInputStream zipInputStream = getZipInputStream();
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (entry.getName().equals(artefactName)) {
                return new FileItem(createFileData(entry), zipInputStream);
            }
        }
        return null;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setListener(Listener callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        return list(name);
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        return read(name);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).build();
    }

    private ZipInputStream getZipInputStream() throws IOException {
        FileItem fileItem = getFileItemWithRetry();
        return new ZipInputStream(fileItem.getStream());
    }

    private FileItem getFileItemWithRetry() throws IOException {
        FileItem fileItem = getFileItem();
        if (fileItem == null) {
            for (int i = 0; i <= GET_FILE_RETRY_ATTEMPTS; i++) {
                ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
                Callable<FileItem> getFileItemTask = this::getFileItem;
                ScheduledFuture<FileItem> schedule = scheduledExecutorService.schedule(getFileItemTask, GET_FILE_RETRY_DELAY_S, TimeUnit.SECONDS);
                try {
                    fileItem = (schedule.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.warn(e.getMessage(), e);
                }

                scheduledExecutorService.shutdown();
                if (fileItem != null) {
                    break;
                }
            }
        }
        if (fileItem == null) {
            log.error("File was not found and all attempts to retrieve it was exhausted.");
            throw new FileNotFoundException("File was not found and all attempts to retrieve it was exhausted.");
        }
        return fileItem;
    }

    private FileItem getFileItem() throws IOException {
        return version == null ? delegate.read(zipPath) : delegate.readHistory(zipPath, version);
    }

    private FileData createFileData(ZipEntry entry) {
        FileData fileData = new FileData();
        fileData.setName(zipPath + "/" + entry.getName());
        fileData.setSize(entry.getSize());
        fileData.setModifiedAt(new Date(entry.getTime()));
        fileData.setComment(entry.getComment());
        return fileData;
    }
}
