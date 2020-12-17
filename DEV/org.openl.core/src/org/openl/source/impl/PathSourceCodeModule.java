package org.openl.source.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.openl.util.RuntimeExceptionWrapper;

public class PathSourceCodeModule extends ASourceCodeModule {

    private final Path path;

    public PathSourceCodeModule(Path path) {
        this.path = Objects.requireNonNull(path, "Path is null.");
    }

    @Override
    public InputStream getByteStream() {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    }

    @Override
    protected String makeUri() {
        return path.toUri().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathSourceCodeModule)) {
            return false;
        }
        PathSourceCodeModule that = (PathSourceCodeModule) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return makeUri();
    }
}
