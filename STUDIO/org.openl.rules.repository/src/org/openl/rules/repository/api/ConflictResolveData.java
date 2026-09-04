package org.openl.rules.repository.api;

import java.util.Iterator;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConflictResolveData implements AdditionalData<ConflictResolveData> {
    @Getter
    private final String commitToMerge;
    @Getter
    private final Iterable<FileItem> resolvedFiles;
    @Getter
    private final String mergeMessage;

    @Override
    public ConflictResolveData convertPaths(final Function<String, String> converter) {
        Iterable<FileItem> convertedFolders = () -> new Iterator<FileItem>() {
            private final Iterator<FileItem> delegate = resolvedFiles.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public FileItem next() {
                var oldPath = delegate.next();
                var data = oldPath.getData();
                data.setName(converter.apply(oldPath.getData().getName()));
                return new FileItem(data, oldPath.getStream());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
        return new ConflictResolveData(commitToMerge, convertedFolders, mergeMessage);
    }
}
