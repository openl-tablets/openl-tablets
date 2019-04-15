package org.openl.rules.repository.api;

import java.io.InputStream;

public class FileChange {
    private final FileData data;
    private final InputStream stream;

    public FileChange(String name, InputStream stream) {
        this.data = new FileData();
        data.setName(name);

        this.stream = stream;
    }

    public FileChange(FileData data, InputStream stream) {
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
