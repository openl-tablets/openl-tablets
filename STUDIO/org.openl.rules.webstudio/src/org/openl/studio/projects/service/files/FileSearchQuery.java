package org.openl.studio.projects.service.files;

import java.util.Set;

import lombok.Builder;
import lombok.Singular;

/**
 * Query criteria for searching project files and folders.
 *
 * @author Yury Molchan
 */
@Builder
public record FileSearchQuery(

        String pattern,

        String content,

        @Singular
        Set<String> extensions,

        FileType type,

        boolean recursive,

        Scope scope,

        String from,

        String version

) {

    /**
     * Where the search runs.
     */
    public enum Scope {

        /**
         * Search down within the mount subtree.
         */
        SUBTREE,

        /**
         * Walk up from {@code from} to the repository root, returning matches nearest first.
         */
        ANCESTORS
    }

    /**
     * Which kind of entries to return.
     */
    public enum FileType {
        FILE,
        FOLDER,
        ANY
    }

    public FileSearchQuery {
        extensions = extensions != null ? Set.copyOf(extensions) : Set.of();
        type = type != null ? type : FileType.ANY;
        scope = scope != null ? scope : Scope.SUBTREE;
    }
}
