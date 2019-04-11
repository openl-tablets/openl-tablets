package org.openl.rules.repository.api;

import java.io.InputStream;

public class FileChange {
    private final String name;
    private final InputStream stream;
    private final String uniqueId;

    public FileChange(String name, InputStream stream) {
        this.name = name;
        this.stream = stream;
        uniqueId = null;
    }

    public FileChange(String name, InputStream stream, String uniqueId) {
        this.name = name;
        this.stream = stream;
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getUniqueId() {
        return uniqueId;
    }
}
