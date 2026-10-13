package org.openl.rules.repository.api;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

/**
 * A file one revision of a branch changes against another.
 *
 * <p>The path is the path of the file from the root of the repository. A file moved to another path is a file
 * deleted at its old path and added at its new one.
 *
 * @param path the path of the file
 * @param kind what became of the file
 */
public record FileChange(@NonNull String path, @NonNull Kind kind) {

    public FileChange {
        Objects.requireNonNull(path);
        Objects.requireNonNull(kind);
    }

    /**
     * What became of a file.
     */
    public enum Kind {
        ADDED,
        MODIFIED,
        DELETED
    }
}
