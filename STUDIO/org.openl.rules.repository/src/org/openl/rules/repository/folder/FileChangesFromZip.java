package org.openl.rules.repository.folder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.api.FileItem;
import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChangesFromZip implements Iterable<FileItem> {
    private static final Logger LOG = LoggerFactory.getLogger(FileChangesFromZip.class);
    private final ZipInputStream stream;
    private final Path folderTo;

    public FileChangesFromZip(ZipInputStream stream, String folderTo) {
        this.stream = stream;
        this.folderTo = Paths.get(folderTo);
    }

    @Override
    public Iterator<FileItem> iterator() {
        return new Iterator<FileItem>() {
            private ZipEntry entry;

            @Override
            public boolean hasNext() {
                try {
                    do {
                        entry = stream.getNextEntry();
                    } while (entry != null && entry.isDirectory());
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    entry = null;
                }

                return entry != null;
            }

            @Override
            public FileItem next() {
                String fullPath;
                try {
                    fullPath = FileUtils.resolveValidPath(folderTo, entry.getName()).toString();
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
                return new FileItem(FileUtils.normalizePath(fullPath), stream);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

}
