package org.openl.rules.repository.api;

import java.io.InputStream;

/**
 * @author Yury Molchan
 */
public class FileItem {
    private FileData data;
    private InputStream stream;

    public FileItem(FileData data, InputStream stream) {
        assert data != null;
        assert stream != null;
        this.data = data;
        this.stream = stream;
    }

    public FileData getData() {
        return data;
    }

    public InputStream getStream() {
        return stream;
    }
}
