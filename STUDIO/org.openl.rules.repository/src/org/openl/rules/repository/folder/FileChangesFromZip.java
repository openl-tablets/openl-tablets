package org.openl.rules.repository.folder;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.api.FileChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChangesFromZip implements Iterable<FileChange> {
    private final Logger log = LoggerFactory.getLogger(FileChangesFromZip.class);
    private final ZipInputStream stream;
    private final String folderTo;

    public FileChangesFromZip(ZipInputStream stream, String folderTo) {
        this.stream = stream;
        this.folderTo = folderTo;
    }

    @Override
    public Iterator<FileChange> iterator() {
        return new Iterator<FileChange>() {
            private ZipEntry entry;

            @Override
            public boolean hasNext() {
                try {
                    do {
                        entry = stream.getNextEntry();
                    } while (entry != null && entry.isDirectory());
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    entry = null;
                }

                return entry != null;
            }

            @Override
            public FileChange next() {
                return new FileChange(folderTo + "/" + entry.getName(), stream);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

}
