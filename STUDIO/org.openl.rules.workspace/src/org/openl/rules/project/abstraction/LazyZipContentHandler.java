package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

class LazyZipContentHandler implements ContentHandler {
    private final Repository repository;
    private final String folderPath;
    private final String entryName;
    private final String version;

    public LazyZipContentHandler(Repository repository, String folderPath, String entryName, String version) {
        this.repository = repository;
        this.folderPath = folderPath;
        this.entryName = entryName;
        this.version = version;
    }

    @Override
    public InputStream loadContent() {
        try {
            FileItem fileItem = version == null ? repository.read(folderPath) : repository.readHistory(folderPath, version);
            ZipInputStream zipInputStream = new ZipInputStream(fileItem.getStream());
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return zipInputStream;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        throw new IllegalArgumentException("Can't find entry " + entryName);
    }

}
