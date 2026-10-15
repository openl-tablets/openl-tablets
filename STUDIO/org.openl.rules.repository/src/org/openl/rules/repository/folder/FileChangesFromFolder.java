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

import org.openl.rules.repository.api.FileItem;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

/**
 * The files under a folder, as changes to write into a repository folder.
 *
 * <p>Each file is named by its path under the folder walked, with {@code /} between the names, after the repository
 * folder it is written to. The folder walked may be the root of an archive or a folder of any file system.
 */
public class FileChangesFromFolder implements Iterable<FileItem>, AutoCloseable {

    private static final Predicate<Path> ACCEPT_ALL = path -> true;

    private final Path walkRoot;
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
        this(walkRoot, StringUtils.EMPTY, filter, fileAdaptors);
    }

    public FileChangesFromFolder(Path walkRoot,
                                 String folderTo,
                                 Predicate<Path> filter,
                                 FileAdaptor... fileAdaptors) throws IOException {
        this.walkRoot = walkRoot;
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
                    final var path = it.next();
                    InputStream is = Files.newInputStream(path);
                    Optional<FileAdaptor> fileAdaptor = fileAdaptors.stream()
                            .filter(adaptor -> adaptor.accept(path))
                            .findFirst();
                    if (fileAdaptor.isPresent()) {
                        is = fileAdaptor.get().apply(is);
                    }
                    return new FileItem(folderTo + nameOf(path), is);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
            }
        };
    }

    /** The path of a file under the folder walked, starting with {@code /}, whatever the file system separates by. */
    private String nameOf(Path path) {
        var name = new StringBuilder();
        for (var segment : walkRoot.relativize(path)) {
            name.append('/').append(segment);
        }
        return name.toString();
    }

    @Override
    public void close() {
        stream.close();
    }
}
