package org.openl.rules.repository.folder;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
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
            private boolean walked;

            @Override
            public boolean hasNext() {
                walkToTheNextFile();
                return entry != null;
            }

            @Override
            public FileItem next() {
                walkToTheNextFile();
                if (entry == null) {
                    throw new NoSuchElementException();
                }
                var name = entry.getName();
                // The archive is read no further until the next question is asked, so the stream stays on
                // the file this item is answered with.
                entry = null;
                walked = false;
                return new FileItem(folderTo + "/" + name, stream);
            }

            /**
             * Walks the archive on to the file it answers with next.
             *
             * <p>The file already walked to is kept until it is answered with, so that asking twice does not
             * read past it.
             */
            private void walkToTheNextFile() {
                if (walked) {
                    return;
                }
                walked = true;
                try {
                    do {
                        entry = stream.getNextEntry();
                    } while (entry != null && entry.isDirectory());
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    entry = null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

}
