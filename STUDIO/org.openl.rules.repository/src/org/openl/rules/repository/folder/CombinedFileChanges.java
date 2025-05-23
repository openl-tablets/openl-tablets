package org.openl.rules.repository.folder;

import java.util.Collection;
import java.util.Iterator;

import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;

/**
 * Used to combine several FileItem iterables into one. For example if we have several iterables from different folders,
 * and we want save them in one transaction, we can combine them with this class and pass the result into the method
 * save(fileData, iterable, changesetType).
 */
public class CombinedFileChanges implements Iterable<FileItem> {
    private final Collection<Iterable<FileItem>> multipleIterables;

    public CombinedFileChanges(Collection<Iterable<FileItem>> multipleIterables) {
        this.multipleIterables = multipleIterables;
    }

    @Override
    public Iterator<FileItem> iterator() {
        Iterator<Iterable<FileItem>> iterableIterator = multipleIterables.iterator();
        return new Iterator<FileItem>() {
            private Iterable<FileItem> fileItemIterable;
            private Iterator<FileItem> fileItemIterator;

            @Override
            public boolean hasNext() {
                if (fileItemIterator != null && fileItemIterator.hasNext()) {
                    return true;
                }

                while (iterableIterator.hasNext()) {
                    if (fileItemIterable instanceof AutoCloseable) {
                        IOUtils.closeQuietly((AutoCloseable) fileItemIterable);
                    }
                    fileItemIterable = iterableIterator.next();
                    fileItemIterator = fileItemIterable.iterator();
                    if (fileItemIterator.hasNext()) {
                        return true;
                    }
                }
                if (fileItemIterable instanceof AutoCloseable) {
                    IOUtils.closeQuietly((AutoCloseable) fileItemIterable);
                }

                return false;
            }

            @Override
            public FileItem next() {
                return fileItemIterator.next();
            }
        };
    }
}
