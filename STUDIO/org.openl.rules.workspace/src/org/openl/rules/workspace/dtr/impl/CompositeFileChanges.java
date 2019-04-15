package org.openl.rules.workspace.dtr.impl;

import java.util.Arrays;
import java.util.Iterator;

import org.openl.rules.repository.api.FileChange;

public class CompositeFileChanges implements Iterable<FileChange> {
    private final Iterable<FileChange> first;
    private final Iterable<FileChange> second;

    public CompositeFileChanges(Iterable<FileChange> first, FileChange... second) {
        this(first, Arrays.asList(second));
    }

    public CompositeFileChanges(Iterable<FileChange> first, Iterable<FileChange> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Iterator<FileChange> iterator() {
        return new Iterator<FileChange>() {
            private Iterator<FileChange> firstIterator = first.iterator();
            private Iterator<FileChange> secondIterator = second.iterator();

            @Override
            public boolean hasNext() {
                if (firstIterator != null) {
                    if (firstIterator.hasNext()) {
                        return true;
                    } else {
                        firstIterator = null;
                    }
                }

                return secondIterator.hasNext();
            }

            @Override
            public FileChange next() {
                return firstIterator != null ? firstIterator.next() : secondIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }
}
