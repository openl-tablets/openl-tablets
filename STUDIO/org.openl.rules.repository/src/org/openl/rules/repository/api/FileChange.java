package org.openl.rules.repository.api;

import java.io.InputStream;

public class FileChange {
    private final String name;
    private final InputStream stream;

    public FileChange(String name, InputStream stream) {
        this.name = name;
        this.stream = stream;
    }

    public String getName() {
        return name;
    }

    public InputStream getStream() {
        return stream;
    }
}
