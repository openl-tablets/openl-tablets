package org.openl.rules.repository.folder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.RuntimeExceptionWrapper;

public class FileChangesFromFolder implements Iterable<FileItem>, AutoCloseable {

    private static final Predicate<Path> ACCEPT_ALL = path -> true;

    private final Stream<Path> stream;
    private final String folderTo;
    private final List<FileAdaptor> fileAdaptors;

    public FileChangesFromFolder(Path walkRoot) throws IOException {
        this(walkRoot, StringUtils.EMPTY);
    }

    public FileChangesFromFolder(Path walkRoot, String folderTo) throws IOException {
        this(walkRoot, folderTo, ACCEPT_ALL);
    }

    public FileChangesFromFolder(Path walkRoot, String folderTo, Predicate<Path> filter) throws IOException {
        this(walkRoot, folderTo, filter, FileAdaptor.EMPTY);
    }

    public FileChangesFromFolder(Path walkRoot,
                                 Predicate<Path> filter,
                                 FileAdaptor... fileAdaptors) throws IOException {
        this(walkRoot, StringUtils.EMPTY, filter,fileAdaptors);
    }

    public FileChangesFromFolder(Path walkRoot,
            String folderTo,
            Predicate<Path> filter,
            FileAdaptor... fileAdaptors) throws IOException {
        this.stream = Files.walk(walkRoot).filter(p -> !walkRoot.equals(p)).filter(Files::isRegularFile).filter(filter);
        this.folderTo = folderTo;
        this.fileAdaptors = Arrays.asList(fileAdaptors);
    }

    @Override
    public Iterator<FileItem> iterator() {
        return new Iterator<FileItem>() {
            private final Iterator<Path> it = stream.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public FileItem next() {
                try {
                    final Path path = it.next();
                    InputStream is = Files.newInputStream(path);
                    Optional<FileAdaptor> fileAdaptor = fileAdaptors.stream()
                        .filter(adaptor -> adaptor.accept(path))
                        .findFirst();
                    if (fileAdaptor.isPresent()) {
                        is = fileAdaptor.get().apply(is);
                    }
                    return new FileItem(folderTo + path, is);
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
            }
        };
    }

    @Override
    public void close() {
        stream.close();
    }
}
