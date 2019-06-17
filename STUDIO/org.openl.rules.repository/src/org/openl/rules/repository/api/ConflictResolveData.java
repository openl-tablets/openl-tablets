package org.openl.rules.repository.api;

import java.util.Iterator;

public class ConflictResolveData implements AdditionalData<ConflictResolveData> {
    private final String commitToMerge;
    private final Iterable<FileChange> resolvedFiles;
    private final String mergeMessage;

    public ConflictResolveData(String commitToMerge, Iterable<FileChange> resolvedFiles, String mergeMessage) {
        this.commitToMerge = commitToMerge;
        this.resolvedFiles = resolvedFiles;
        this.mergeMessage = mergeMessage;
    }

    public String getCommitToMerge() {
        return commitToMerge;
    }

    public Iterable<FileChange> getResolvedFiles() {
        return resolvedFiles;
    }

    public String getMergeMessage() {
        return mergeMessage;
    }

    @Override
    public ConflictResolveData convertPaths(final PathConverter converter) {
        Iterable<FileChange> convertedFolders = new Iterable<FileChange>() {
            @Override
            public Iterator<FileChange> iterator() {
                return new Iterator<FileChange>() {
                    private final Iterator<FileChange> delegate = resolvedFiles.iterator();

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public FileChange next() {
                        FileChange oldPath = delegate.next();
                        FileData data = oldPath.getData();
                        data.setName(converter.convert(oldPath.getData().getName()));
                        return new FileChange(data, oldPath.getStream());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Remove is not supported");
                    }
                };
            }
        };
        return new ConflictResolveData(commitToMerge, convertedFolders,mergeMessage);
    }
}
