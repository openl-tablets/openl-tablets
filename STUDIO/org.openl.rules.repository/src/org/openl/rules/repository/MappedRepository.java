package org.openl.rules.repository;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.rules.repository.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappedRepository implements FolderRepository, Closeable {
    private final Logger log = LoggerFactory.getLogger(MappedRepository.class);

    private FolderRepository delegate;

    private volatile Map<String, String> externalToInternal = Collections.emptyMap();

    private ReadWriteLock mappingLock = new ReentrantReadWriteLock();

    public void setDelegate(FolderRepository delegate) {
        this.delegate = delegate;
    }

    public FolderRepository getDelegate() {
        return delegate;
    }

    public void setExternalToInternal(Map<String, String> externalToInternal) {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            this.externalToInternal = Collections.unmodifiableMap(externalToInternal);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            externalToInternal = Collections.emptyMap();

            if (delegate instanceof Closeable) {
                ((Closeable) delegate).close();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        Map<String, String> mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String external = entry.getKey();
            if (external.startsWith(path)) {
                internal.addAll(delegate.list(entry.getValue() + "/"));
            } else if (path.startsWith(external + "/")) {
                internal.addAll(delegate.list(toInternal(mapping, path)));
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public FileData check(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.check(toInternal(mapping, name)));
    }

    @Override
    public FileItem read(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
    }

    @Override
    public boolean delete(FileData data) {
        Map<String, String> mapping = getMappingForRead();
        return delegate.delete(toInternal(mapping, data));
    }

    @Override
    public FileData copy(String srcName, FileData destData) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.copy(toInternal(mapping, srcName), toInternal(mapping, destData)));
    }

    @Override
    public FileData rename(String srcName, FileData destData) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.rename(toInternal(mapping, srcName), toInternal(mapping, destData)));
    }

    @Override
    public void setListener(final Listener callback) {
        delegate.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.readHistory(toInternal(mapping, name), version));
    }

    @Override
    public boolean deleteHistory(FileData data) {
        Map<String, String> mapping = getMappingForRead();
        return delegate.deleteHistory(toInternal(mapping, data));
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        Map<String, String> mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String external = entry.getKey();
            if (external.startsWith(path) && !external.substring(path.length()).contains("/")) {
                // "external" is direct child of "path"
                FileData data = delegate.check(entry.getValue());
                if (data == null) {
                    throw new IOException("Can't find " + entry.getValue());
                }
                internal.add(data);
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.listFiles(toInternal(mapping, path), version));
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileChange> files) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.save(toInternal(mapping, folderData), toInternal(mapping, files)));
    }

    private Map<String, String> getMappingForRead() {
        Lock lock = mappingLock.readLock();
        Map<String, String> mapping;
        try {
            lock.lock();
            mapping = externalToInternal;
        } finally {
            lock.unlock();
        }
        return mapping;
    }

    private Iterable<FileChange> toInternal(final Map<String, String> mapping, final Iterable<FileChange> files) {
        return new Iterable<FileChange>() {
            @SuppressWarnings("NullableProblems")
            @Override
            public Iterator<FileChange> iterator() {
                return new Iterator<FileChange>() {
                    private final Iterator<FileChange> delegate = files.iterator();

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public FileChange next() {
                        FileChange external = delegate.next();
                        return new FileChange(toInternal(mapping, external.getName()), external.getStream());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Remove is not supported");
                    }
                };
            }
        };
    }

    private FileData toInternal(Map<String, String> externalToInternal, FileData data) {
        FileData fileData = copy(data);
        fileData.setName(toInternal(externalToInternal, data.getName()));
        return fileData;
    }

    private String toInternal(Map<String, String> externalToInternal, String externalPath) {
        for (Map.Entry<String, String> entry : externalToInternal.entrySet()) {
            String externalBase = entry.getKey();
            if (externalPath.equals(externalBase) || externalPath.startsWith(externalBase + "/")) {
                return entry.getValue() + externalPath.substring(externalBase.length());
            }
        }

        log.warn("Mapped folder for " + externalPath + " not found. Use it as is.");
        return externalPath;
    }

    private List<FileData> toExternal(Map<String, String> externalToInternal, List<FileData> internal) {
        List<FileData> external = new ArrayList<>(internal.size());

        for (FileData data : internal) {
            external.add(toExternal(externalToInternal, data));
        }

        return external;
    }

    private FileItem toExternal(Map<String, String> externalToInternal, FileItem internal) {
        return new FileItem(toExternal(externalToInternal, internal.getData()), internal.getStream());
    }

    private FileData toExternal(Map<String, String> externalToInternal, FileData data) {
        if (data == null) {
            return null;
        }

        FileData fileData = copy(data);
        fileData.setName(toExternal(externalToInternal, data.getName()));
        return fileData;
    }

    private String toExternal(Map<String, String> externalToInternal, String internalPath) {
        for (Map.Entry<String, String> entry : externalToInternal.entrySet()) {
            String internalBase = entry.getValue();
            if (internalPath.equals(internalBase) || internalPath.startsWith(internalBase + "/")) {
                return entry.getKey() + internalPath.substring(internalBase.length());
            }
        }

        // Shouldn't occur. If occurred, it's a bug.
        throw new IllegalStateException("Can't find external path for: " + internalPath);
    }

    private FileData copy(FileData data) {
        FileData copy = new FileData();
        copy.setVersion(data.getVersion());
        copy.setAuthor(data.getAuthor());
        copy.setComment(data.getComment());
        copy.setSize(data.getSize());
        copy.setDeleted(data.isDeleted());
        return copy;
    }
}
