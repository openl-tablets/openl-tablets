package org.openl.rules.repository.zip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

class CompoundPath {

    static final String PATH_SEPARATOR = "/";

    /**
     * The folder or the archive of the top level of the repository the path is in, which names start with.
     */
    @Getter
    private final Path top;
    private final Path resolvedPath;
    @Getter(AccessLevel.PACKAGE)
    private final @Nullable Path pathToArchive;
    private BasicFileAttributes attributes;

    CompoundPath(Path top, Path resolvedPath, @Nullable Path pathToArchive) {
        this(top, resolvedPath, pathToArchive, null);
    }

    CompoundPath(Path top,
                 Path resolvedPath,
                 @Nullable Path pathToArchive,
                 @Nullable BasicFileAttributes attributes) {
        this.top = Objects.requireNonNull(top);
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

    boolean isDirectory() throws IOException {
        return getAttributes().isDirectory();
    }

    boolean isRegularFile() throws IOException {
        return getAttributes().isRegularFile();
    }

    boolean exists() {
        return Files.exists(resolvedPath);
    }

    Date getModifiedAt() throws IOException {
        return new Date(getAttributes().lastModifiedTime().toMillis());
    }

    long getSize() throws IOException {
        return getAttributes().size();
    }

    /**
     * The name of the path in the repository: the name of its folder or archive of the top level, followed by the
     * path inside it.
     *
     * @param alias the names of the folders and the archives of the top level
     */
    String relativize(Function<Path, String> alias) {
        var local = pathToArchive != null ? pathToArchive : resolvedPath;
        var name = new StringBuilder(alias.apply(top));
        if (!local.equals(top)) {
            name.append(PATH_SEPARATOR).append(top.relativize(local));
        }
        if (pathToArchive != null) {
            var pathInArchive = StringUtils.removeStart(resolvedPath.toString(), PATH_SEPARATOR);
            if (!pathInArchive.isEmpty()) {
                name.append(PATH_SEPARATOR).append(pathInArchive);
            }
        }
        return name.toString().replace('\\', PATH_SEPARATOR.charAt(0));
    }
}
