package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.api.*;
import org.openl.util.IOUtils;

/**
 * Treat zip files as separate repositories
 */
class ZipFolderRepository implements Repository {
    private final Repository delegate;
    private final String zipPath;
    private final String version;

    ZipFolderRepository(Repository delegate, String zipPath, String version) {
        this.delegate = delegate;
        this.zipPath = zipPath;
        this.version = version;
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
    public boolean delete(FileData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData copy(String srcName, FileData destData) throws IOException {
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
    public boolean deleteHistory(FileData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Features supports() {
        return new Features(this);
    }

    private ZipInputStream getZipInputStream() throws IOException {
        FileItem fileItem = version == null ? delegate.read(zipPath) : delegate.readHistory(zipPath, version);
        return new ZipInputStream(fileItem.getStream());
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
