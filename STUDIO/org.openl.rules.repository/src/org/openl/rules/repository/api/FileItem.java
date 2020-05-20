package org.openl.rules.repository.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yury Molchan
 */
public class FileItem implements Closeable {
    private FileData data;
    private InputStream stream;

    /**
     * @param name the full path name from the root folder.
     * @param stream the stream for the file. The file is deleted if stream is null.
     */
    public FileItem(String name, InputStream stream) {
        this.data = new FileData();
        data.setName(name);

        this.stream = stream;
    }

    /**
     * @param data the file descriptor
     * @param stream the stream for the file. The file is deleted if stream is null.
     */
    public FileItem(FileData data, InputStream stream) {
        assert data != null;
        this.data = data;
        this.stream = stream;
    }

    public FileData getData() {
        return data;
    }

    public InputStream getStream() {
        return stream;
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }
}
