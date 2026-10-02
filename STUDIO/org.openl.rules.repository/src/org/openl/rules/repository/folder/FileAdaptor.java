package org.openl.rules.repository.folder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileAdaptor {

    FileAdaptor[] EMPTY = new FileAdaptor[0];

    boolean accept(Path path);

    InputStream apply(InputStream inputStream) throws IOException;
}
