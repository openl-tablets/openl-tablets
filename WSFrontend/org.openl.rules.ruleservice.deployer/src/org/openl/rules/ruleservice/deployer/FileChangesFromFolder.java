package org.openl.rules.ruleservice.deployer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import org.openl.rules.repository.api.FileItem;
import org.openl.util.RuntimeExceptionWrapper;

class FileChangesFromFolder implements Iterable<FileItem>, AutoCloseable {

    private final Stream<Path> stream;
    private final String folderTo;

    public FileChangesFromFolder(Path folder, String folderTo) throws IOException {
        this.stream = Files.walk(folder).filter(Files::isRegularFile);
        this.folderTo = folderTo;
    }

    @Override
    public Iterator<FileItem> iterator() {
        return new Iterator<FileItem>() {
            private final Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public FileItem next() {
                try {
                    Path path = it.next();
                    return new FileItem(folderTo + path.toString(), Files.newInputStream(path));
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
