package org.openl.rules.repository.folder;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.repository.api.FileItem;

@RequiredArgsConstructor
@Slf4j
public class FileChangesFromZip implements Iterable<FileItem> {
    private final ZipInputStream stream;
    private final String folderTo;

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
                    log.error(e.getMessage(), e);
                    entry = null;
                }

                return entry != null;
            }

            @Override
            public FileItem next() {
                return new FileItem(folderTo + "/" + entry.getName(), stream);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

}
