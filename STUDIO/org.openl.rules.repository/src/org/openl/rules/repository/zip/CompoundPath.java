package org.openl.rules.repository.zip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Objects;

class CompoundPath {

    static final String PATH_SEPARATOR = "/";

    private final Path resolvedPath;
    private final Path pathToArchive;
    private BasicFileAttributes attributes;

    CompoundPath(Path resolvedPath, Path pathToArchive) {
        this(resolvedPath, pathToArchive, null);
    }

    CompoundPath(Path resolvedPath, Path pathToArchive, BasicFileAttributes attributes) {
        this.resolvedPath = Objects.requireNonNull(resolvedPath);
        this.pathToArchive = pathToArchive;
        this.attributes = attributes;
    }

    private BasicFileAttributes getAttributes() throws IOException {
        if (attributes == null) {
            attributes = Files.readAttributes(resolvedPath, BasicFileAttributes.class);
        }
        return attributes;
    }

    Path getPath() {
        return resolvedPath;
    }

    Path getPathToArchive() {
        return pathToArchive;
    }

    boolean isDirectory() throws IOException {
        return getAttributes().isDirectory();
    }

    boolean isRegularFile() throws IOException {
        return getAttributes().isRegularFile();
    }

    Date getModifiedAt() throws IOException {
        return new Date(getAttributes().lastModifiedTime().toMillis());
    }

    long getSize() throws IOException {
        return getAttributes().size();
    }

    String relativize(Path root) {
        String name;
        if (pathToArchive == null) {
            name = root.relativize(resolvedPath)
                    .toString();
        } else {
            String pathInArchive = resolvedPath.toString();
            if (pathInArchive.charAt(0) == PATH_SEPARATOR.charAt(0)) {
                pathInArchive = pathInArchive.substring(1);
            }
            name = root.relativize(pathToArchive)
                    .resolve(pathInArchive)
                    .toString();
        }
        return name.replace('\\', PATH_SEPARATOR.charAt(0));
    }

}
